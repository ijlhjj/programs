package com.sweetmanor.programs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sweetmanor.utils.DateUtil;

/**
 * 小工具集：查找重复文件
 * 
 * <pre>
 * 
 * 设计思路：
 *     假定：文件大小不同，文件肯定不同
 *         1、获取所有文件；
 *         2、按文件大小进行分组；（分组比对可极大减少比对次数，且方便多线程并行执行）
 *         3、对每个分组进行比对；
 *         4、输出比对结果，可试情况进行文件移动。
 *         
 *     其中，第3步是最耗时的，可以使用多线程并行执行以提高效率
 *     考虑到同名文件重复的可能性更大，比对前按文件名进行排序，以减少重复比对
 *     使用两层 for 循环进行实际比对
 *     
 *     数据结构：
 *         每个比对分组是一个 List<File>
 *         比对结果使用 Map<Integer, List<File>> 存储
 *         分组比对时使用 Set<File> 存储比中重复文件，避免重复比对
 * 
 * </pre>
 * 
 * @version 1.0 2020-2-28
 * @author ijlhjj
 */
public class FindRepeatFile {
	private static final Logger logger = LogManager.getLogger();

	private final String OUT_FILE = "D:\\SameFile.txt";// 比对结果输出文件
	private final String OUT_DIR = "D:\\tempDir";// 重复文件剪切目录。存在同名文件时不能移动到同一个目录

	private AtomicInteger groupKey = new AtomicInteger(); // 比对结果分组键值
	private List<String> paths = new ArrayList<>();// 比对文件所在目录

	public FindRepeatFile(String[] dirArr) {
		for (String dir : dirArr)
			paths.add(dir);
	}

	/**
	 * 执行重复文件比对方法
	 */
	private void execute() {
		long start = System.currentTimeMillis();
		logger.info("比对路径：");
		paths.forEach(logger::info);

		// 获取所有文件
		Collection<File> files = new ArrayList<>();
		for (String path : paths) {
			Collection<File> fileSet = FileUtils.listFiles(new File(path), null, true);
			files.addAll(fileSet);
		}
		logger.info(files.size());

		// 按文件大小分组
		var fileMap = files.stream().collect(Collectors.groupingBy(File::length));

		// 对分组文件进行比对，待优化为多线程比对
		for (List<File> fileList : fileMap.values()) {
			if (fileList.size() < 2)// 分组只有1个文件的不需要比对
				continue;

			// 对当前分组进行比对
			Map<Integer, List<File>> resultMap = compare(fileList);

			// 输出比对结果
			printResult(resultMap);
		}

		long end = System.currentTimeMillis();
		logger.info("比对花费时长：" + DateUtil.convertMillisToString(end - start));
	}

	/**
	 * 实际执行文件比对方法
	 * 
	 * @param files 比对文件列表
	 */
	private Map<Integer, List<File>> compare(List<File> files) {

		// 同名文件重复的可能性更大，按文件名排序以减少后期比对次数
		File[] fileArr = files.stream() //
				.sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))// 按文件名进行排序
				.toArray(File[]::new);

		Map<Integer, List<File>> resultMap = new HashMap<>();// 存放重复文件结果集，可能存在多组重复文件
		Set<File> sameFileSet = new HashSet<>();// 存放重复文件比中集，已比中文件不用再次比对

		// 循环比对每个文件，每个文件都只跟后面的文件进行比对
		for (int i = 0; i < fileArr.length - 1; i++) {// 外层循环从第1个到倒数第2个
			File currFile = fileArr[i];// 当前比对文件
			if (sameFileSet.contains(currFile))// 如果当前文件之前已比中，不用再次进行比对
				continue;

			List<File> sameFileList = new ArrayList<>();// 存放当前比对文件的重复文件
			sameFileList.add(currFile);
			for (int j = i + 1; j < fileArr.length; j++) {// 内层循环从外层加1比到最后
				File compFile = fileArr[j]; // 比对文件
				try {
					if (FileUtils.contentEquals(currFile, compFile)) {// 如果文件内容相同
						sameFileList.add(compFile);// 加入重复文件列表
						sameFileSet.add(compFile);// 加入重复文件比中集
					}
				} catch (IOException e) {
					logger.error("文件比对报错，当前比对文件为：" + currFile.getAbsolutePath() + "\t-\t" + compFile.getAbsolutePath());
					e.printStackTrace();
				}
			}

			// 比中重复文件，加入结果集
			if (sameFileList.size() > 1)
				resultMap.put(groupKey.incrementAndGet(), sameFileList);
		}

		return resultMap;
	}

	/**
	 * 输出比对结果集。涉及文件输出，不支持多线程
	 */
	private void printResult(Map<Integer, List<File>> resultMap) {
		for (Integer index : resultMap.keySet()) {
			List<String> lines = new ArrayList<>();
			lines.add("序号：" + index);// 当前分组序号

			File targetDir = new File(OUT_DIR, index.toString());// 文件移动时使用
			try {
				List<File> files = resultMap.get(index);
				for (File file : files) {
					lines.add("\t" + file.getAbsolutePath()); // 重复文件绝对路径
					// 移动文件。存在同名文件时抛出FileExistsException
					// FileUtils.moveFileToDirectory(file, targetDir, true);
				}
				FileUtils.writeLines(new File(OUT_FILE), lines, true);// 追加方式写入文件
			} catch (IOException e) {
				logger.error("结果写入文件报错：" + lines);
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		if (args == null)
			System.out.println("请配置查找路径！");

		FindRepeatFile app = new FindRepeatFile(args);
		app.execute();// 调用比对方法
	}

}
