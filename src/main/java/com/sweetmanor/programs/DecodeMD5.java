package com.sweetmanor.programs;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sweetmanor.utils.DateUtil;
import com.sweetmanor.utils.SecurityUtil;

/**
 * MD5破解示例：破解字典实时生成，效率有待改进。<br/>
 * 破解字典包括65个字符集，在开发机实际测试性能为: 5位最多耗时9分16秒，按此推算6位最多耗时10.04小时
 * 
 * @version 1.0 2016-11-25
 * @author ijlhjj
 */
public class DecodeMD5 {
	private static final Logger logger = LogManager.getLogger();

	public static final int NUMBERS = 1;// 数字集合
	public static final int LOWER_CHARS = 2;// 小写字母集合
	public static final int UPPER_CHARS = 4;// 大写字母集合
	public static final int SPECIAL_CHARS = 8;// 特殊字符集合
	public static final int ALL_CHARS = 15; // 所有字符集合

	private static final char[] numbers = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' }; // 数字集合
	private static final char[] lowerChars = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
			'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' }; // 小写字母集合
	private static final char[] upperChars = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' }; // 大写字母集合
	private static final char[] specialChars = { '_', '@', '!' }; // 特殊字符集合

	public static int CHAR_LIMIT = 6;
	public static int LOG_PRINT_LIMIT = 10_000_000;

	private static char[] usedChars; // 实际碰撞使用字符集合
	private static int len;// 使用破解字符集的字符数

	private static int tmpCounter; // 临时计数器
	private static int totalCounter; // 总计数器

	private static long startTime; // 比对开始时间
	private static long logTime; // 打印日志时间

	private static String md5Code;// 最终比对MD5值

	/**
	 * 碰撞字符串的MD5信息摘要，碰撞字符上限设置为6位，使用所有字符集合进行碰撞
	 * 
	 * @param md5Str 32位的md5信息摘要
	 * @return 信息摘要对应的原字符串，碰撞不上返回null
	 */
	public static String decode(String md5Str) {
		return decode(md5Str, ALL_CHARS);
	}

	/**
	 * 碰撞字符串的MD5信息摘要，碰撞字符上限设置为6位
	 * 
	 * @param md5Str    32位的md5信息摘要
	 * @param usedArray 使用的字符集合：
	 *                  1-NUMBERS，2-LOWER_CHARS，4-UPPER_CHARS，8-SPECIAL_CHARS。<br>
	 *                  可使用任意字符集合的组合作为碰撞字典，如：NUMBERS | LOWER_CHARS | UPPER_CHARS
	 *                  表示使用大小写字符和数字。如果设置参数非法，则使用全字符集作为碰撞字典
	 * @return 信息摘要对应的原字符串，碰撞不上返回null
	 */
	public static String decode(String md5Str, int usedArray) {
		if (md5Str == null || md5Str.length() != 32)// 如果位数不对，直接返回null
			return null;

		if (usedArray > 15 || usedArray < 1) {// 如果字典参数非法，使用全字符集字典
			usedArray = ALL_CHARS;
		}

		md5Code = md5Str.toLowerCase(); // 将比对字符转换为小写

		usedChars = new char[] {};// 初始化碰撞字符集为空

		// 判断是否使用数字字符集
		int flag = (usedArray & 0b0001) / 0b0001;
		if (flag == 1) {
			usedChars = ArrayUtils.addAll(usedChars, numbers);
		}
		// 判断是否使用小写字母集合
		flag = (usedArray & 0b0010) / 0b0010;
		if (flag == 1) {
			usedChars = ArrayUtils.addAll(usedChars, lowerChars);
		}
		// 判断是否使用大写字母集合
		flag = (usedArray & 0b0100) / 0b0100;
		if (flag == 1) {
			usedChars = ArrayUtils.addAll(usedChars, upperChars);
		}
		// 判断是否使用特殊字符集合
		flag = (usedArray & 0b1000) / 0b1000;
		if (flag == 1) {
			usedChars = ArrayUtils.addAll(usedChars, specialChars);
		}

		len = usedChars.length;

		// 初始化计数器
		tmpCounter = 0;
		totalCounter = 0;
		startTime = System.currentTimeMillis();
		logTime = startTime;

		String source = null;// md5Str对应的字符串

		// 循环测试各长度字典字符
		for (int i = 1; i <= CHAR_LIMIT; i++) {
			StringBuffer sb = new StringBuffer("");
			source = decodeMD5(i, sb);
			if (source != null)
				break;
		}

		long usedTime = System.currentTimeMillis() - startTime;
		logger.debug("程序运行时间：" + DateUtil.convertMillisToString(usedTime));

		if (source == null)
			logger.info("在字典中未找到符合的字符串！");

		return source;
	}

	/**
	 * 实际执行比对的方法，使用了RecursionReplaceFor的递归结构
	 */
	private static String decodeMD5(int n, StringBuffer prefix) {
		if (n == 1) {
			for (int i = 0; i < len; i++) {
				String text = prefix + "" + usedChars[i];
				String code = SecurityUtil.md5(text).toLowerCase();
				if (md5Code.equals(code)) { // 比对成功
					logger.info("比对成功：\t" + text + " - " + code);
					return text;
				}
				// 记录过程日志
				tmpCounter++;
				totalCounter++;
				if (tmpCounter >= LOG_PRINT_LIMIT) {
					long current = System.currentTimeMillis();
					String msg = "已比对 " + (totalCounter / LOG_PRINT_LIMIT) + " 千万次\t";
					msg += "本次消耗时间： " + DateUtil.convertMillisToString(current - logTime) + "\t";
					msg += "总计消耗时间： " + DateUtil.convertMillisToString(current - startTime);
					logger.debug(msg);
					tmpCounter = 0;
					logTime = current;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				StringBuffer temp = new StringBuffer(prefix);
				temp.append(usedChars[i]);
				String yes = decodeMD5(n - 1, temp);// 递归调用
				if (yes != null)
					return yes;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		String code = DecodeMD5.decode("b641f54342d572b2d21d27491206ce29");
		System.out.println(code);
	}

}
