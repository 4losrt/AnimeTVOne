package com.one.animetvone

import com.one.animetvone.util.ChineseConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class ChineseConverterTest {

    @Test
    fun `toTraditionalChinese converts popular anime titles to Traditional Chinese`() {
        assertEquals("進擊的巨人", ChineseConverter.toTraditionalChinese("进击的巨人"))
        assertEquals("鬼滅之刃", ChineseConverter.toTraditionalChinese("鬼灭之刃"))
        assertEquals("咒術迴戰", ChineseConverter.toTraditionalChinese("咒术回战"))
        assertEquals("葬送的芙莉蓮", ChineseConverter.toTraditionalChinese("葬送的芙莉莲"))
        assertEquals("間諜家家酒", ChineseConverter.toTraditionalChinese("间谍过家家"))
        assertEquals("無職轉生", ChineseConverter.toTraditionalChinese("无职转生"))
        assertEquals("孤獨搖滾", ChineseConverter.toTraditionalChinese("孤独摇滚"))
        assertEquals("我推的孩子", ChineseConverter.toTraditionalChinese("我推的孩子"))
        assertEquals("鏈鋸人", ChineseConverter.toTraditionalChinese("链锯人"))
        assertEquals("名偵探柯南", ChineseConverter.toTraditionalChinese("名侦探柯南"))
        assertEquals("航海王", ChineseConverter.toTraditionalChinese("海贼王"))
    }

    @Test
    fun `toTraditionalChinese normalizes episode, season, and resolution terminology`() {
        assertEquals("第 01 集", ChineseConverter.toTraditionalChinese("EP 01"))
        assertEquals("第 1 集", ChineseConverter.toTraditionalChinese("EP 1"))
        assertEquals("第 12 集", ChineseConverter.toTraditionalChinese("第 12 話"))
        assertEquals("全 12 集", ChineseConverter.toTraditionalChinese("全 12 話"))
        assertEquals("高清 1080P", ChineseConverter.toTraditionalChinese("HD 1080P"))
    }

    @Test
    fun `toTraditionalChinese handles null or blank input gracefully`() {
        assertEquals("", ChineseConverter.toTraditionalChinese(null))
        assertEquals("", ChineseConverter.toTraditionalChinese(""))
        assertEquals("", ChineseConverter.toTraditionalChinese("   "))
    }
}
