package com.sweetmanor.programs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DecodeMD5Test {

	/**
	 * 测试全字符集的decode方法
	 */
	@Test
	void testDecodeAllChars() {
		String actual = DecodeMD5.decode("b641f54342d572b2d21d27491206ce29");
		assertNotNull(actual);
		assertEquals("Ok!", actual);
	}

	/**
	 * 测试部分字符集的decode方法，大小写字母集合在未碰撞成功的情况下运行时间较长，在此不单独进行测试
	 */
	@Test
	void testDecodePartChars() {
		String actual = DecodeMD5.decode("b641f54342d572b2d21d27491206ce29", DecodeMD5.NUMBERS);// 测试数字集合
		assertNull(actual);

		// 测试 小写字母集合+大写字母集合+特殊字符集合
		actual = DecodeMD5.decode("b641f54342d572b2d21d27491206ce29",
				DecodeMD5.LOWER_CHARS | DecodeMD5.UPPER_CHARS | DecodeMD5.SPECIAL_CHARS);
		assertNotNull(actual);
		assertEquals("Ok!", actual);
	}

}
