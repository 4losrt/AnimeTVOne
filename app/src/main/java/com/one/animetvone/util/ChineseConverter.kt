package com.one.animetvone.util

object ChineseConverter {

    private val phraseMap = mapOf(
        "进击的巨人" to "進擊的巨人",
        "鬼灭之刃" to "鬼滅之刃",
        "咒术回战" to "咒術迴戰",
        "葬送的芙莉莲" to "葬送的芙莉蓮",
        "间谍过家家" to "間諜家家酒",
        "SPY×FAMILY间谍过家家" to "SPY×FAMILY 間諜家家酒",
        "无职转生" to "無職轉生",
        "孤独摇滚" to "孤獨搖滾",
        "我推的孩子" to "我推的孩子",
        "链锯人" to "鏈鋸人",
        "电锯人" to "鏈鋸人",
        "名侦探柯南" to "名偵探柯南",
        "海贼王" to "航海王",
        "刀剑神域" to "刀劍神域",
        "机动战士高达" to "機動戰士鋼彈",
        "水星的魔女" to "水星的魔女",
        "影之实力者" to "影之強者",
        "药屋少女的独语" to "藥師少女的獨語",
        "药师少女的独语" to "藥師少女的獨語",
        "重新尝试" to "重試",
        "重新加载" to "重試",
        "尴尬" to "尷尬",
        "第1季" to "第一季",
        "第2季" to "第二季",
        "第3季" to "第三季",
        "第4季" to "第四季",
        "第5季" to "第五季",
        "第6季" to "第六季",
        "第7季" to "第七季",
        "第8季" to "第八季",
        "第9季" to "第九季",
    )

    private val charPairs: List<Pair<Char, Char>> = listOf(
        '万' to '萬', '与' to '與', '丑' to '醜', '业' to '業', '丛' to '叢',
        '东' to '東', '丝' to '絲', '丢' to '丟', '两' to '兩', '严' to '嚴',
        '丧' to '喪', '个' to '個', '丰' to '豐', '临' to '臨', '丽' to '麗',
        '举' to '舉', '么' to '麼', '义' to '義', '乌' to '烏', '乐' to '樂',
        '乔' to '喬', '习' to '習', '乡' to '鄉', '书' to '書', '买' to '買',
        '乱' to '亂', '争' to '爭', '于' to '於', '亏' to '虧', '云' to '雲',
        '亚' to '亞', '产' to '產', '亩' to '畝', '亲' to '親', '亿' to '億',
        '仆' to '僕', '仓' to '倉', '仪' to '儀', '们' to '們', '价' to '價',
        '众' to '眾', '优' to '優', '伙' to '夥', '会' to '會', '伞' to '傘',
        '伟' to '偉', '传' to '傳', '伤' to '傷', '伦' to '倫', '伪' to '偽',
        '体' to '體', '余' to '餘', '侠' to '俠', '侥' to '僥', '侦' to '偵',
        '侧' to '側', '侨' to '僑', '侩' to '儈', '俨' to '儼', '俩' to '倆',
        '俪' to '儷', '俭' to '儉', '债' to '債', '倾' to '傾', '偿' to '償',
        '储' to '儲', '儿' to '兒', '兖' to '兗', '党' to '黨', '兰' to '蘭',
        '关' to '關', '兴' to '興', '兹' to '茲', '养' to '養', '兽' to '獸',
        '垅' to '壟', '冲' to '衝', '决' to '決', '况' to '況', '冻' to '凍',
        '净' to '淨', '凄' to '淒', '准' to '準', '凉' to '涼', '减' to '減',
        '凑' to '湊', '凛' to '凜', '几' to '幾', '凤' to '鳳', '凭' to '憑',
        '凯' to '凱', '击' to '擊', '凿' to '鑿', '划' to '劃', '刘' to '劉',
        '则' to '則', '刚' to '剛', '创' to '創', '删' to '刪', '别' to '別',
        '劫' to '劫', '剧' to '劇', '劝' to '勸', '办' to '辦', '务' to '務',
        '动' to '動', '励' to '勵', '劲' to '勁', '劳' to '勞', '势' to '勢',
        '勋' to '勳', '匀' to '勻', '区' to '區', '医' to '醫', '华' to '華',
        '协' to '協', '单' to '單', '卖' to '賣', '卢' to '盧', '卫' to '衛',
        '却' to '卻', '厂' to '廠', '厅' to '廳', '历' to '歷', '厉' to '厲',
        '压' to '壓', '厌' to '厭', '厕' to '廁', '厘' to '釐', '厢' to '廂',
        '厦' to '廈', '厨' to '廚', '厩' to '廄', '厮' to '廝', '县' to '縣',
        '参' to '參', '双' to '雙', '发' to '發', '变' to '變', '叙' to '敘',
        '叠' to '疊', '叶' to '葉', '号' to '號', '叹' to '嘆', '吁' to '籲',
        '吓' to '嚇', '吕' to '呂', '吗' to '嗎', '吨' to '噸', '听' to '聽',
        '启' to '啟', '吴' to '吳', '呐' to '吶', '呕' to '嘔', '呗' to '唄',
        '员' to '員', '呛' to '嗆', '呜' to '嗚', '咏' to '詠', '咙' to '嚨',
        '咛' to '嚀', '响' to '響', '哑' to '啞', '哒' to '噠', '哔' to '嗶',
        '哗' to '嘩', '哙' to '噲', '啸' to '嘯', '哺' to '哺', '唠' to '嘮',
        '唤' to '喚', '啧' to '嘖', '啬' to '薔', '啭' to '囀', '啰' to '囉',
        '喷' to '噴', '喽' to '嘍', '嘤' to '嚶', '嘱' to '囑', '团' to '團',
        '园' to '園', '围' to '圍', '国' to '國', '图' to '圖', '圆' to '圓',
        '圣' to '聖', '场' to '場', '坏' to '壞', '块' to '塊', '坚' to '堅',
        '坛' to '壇', '垒' to '壘', '垦' to '墾', '执' to '執', '涡' to '渦',
        '堑' to '塹', '堕' to '墮', '墙' to '牆', '壮' to '壯', '声' to '聲',
        '壳' to '殼', '壶' to '壺', '处' to '處', '备' to '備', '复' to '複',
        '够' to '夠', '头' to '頭', '夸' to '誇', '夹' to '夾', '夺' to '奪',
        '奂' to '奐', '奋' to '奮', '奖' to '獎', '奥' to '奧', '妆' to '妝',
        '妇' to '婦', '妈' to '媽', '韧' to '韌', '妩' to '嫵', '妪' to '媼',
        '妫' to '嬀', '娱' to '娛', '娲' to '媧', '娴' to '嫻', '婴' to '嬰',
        '婵' to '嬋', '婶' to '嬸', '媪' to '媼', '嫒' to '嬡', '嫔' to '嬪',
        '嬷' to '嬤', '孙' to '孫', '学' to '學', '孪' to '攣', '宁' to '寧',
        '宝' to '寶', '实' to '實', '宠' to '寵', '审' to '審', '宪' to '憲',
        '宫' to '宮', '写' to '寫', '宽' to '寬', '寰' to '寰', '对' to '對',
        '寻' to '尋', '导' to '導', '寿' to '壽', '封' to '封', '射' to '射',
        '将' to '將', '尉' to '尉', '尊' to '尊', '尔' to '爾', '尘' to '塵',
        '尚' to '尚', '尝' to '嘗', '尧' to '堯', '尸' to '屍', '尽' to '盡',
        '局' to '局', '居' to '居', '届' to '屆', '屈' to '屈', '屋' to '屋',
        '层' to '層', '履' to '履', '属' to '屬', '岁' to '歲', '岂' to '豈',
        '岗' to '崗', '岘' to '峴', '岚' to '嵐', '岛' to '島', '岭' to '嶺',
        '岳' to '嶽', '峡' to '峽', '峥' to '崢', '峦' to '巒', '崭' to '嶄',
        '嵘' to '嶸', '巩' to '鞏', '帅' to '帥', '师' to '師', '帐' to '帳',
        '帘' to '簾', '带' to '帶', '帧' to '幀', '帮' to '幫', '幞' to '幞',
        '幂' to '冪', '广' to '廣', '庄' to '莊', '庆' to '慶', '炉' to '爐',
        '庐' to '廬', '库' to '庫', '应' to '應', '庙' to '廟', '庞' to '龐',
        '废' to '廢', '度' to '度', '座' to '座', '庭' to '庭', '廉' to '廉',
        '廊' to '廊', '厨' to '廚', '厮' to '廝', '厅' to '廳', '开' to '開',
        '异' to '異', '弃' to '棄', '弑' to '弒', '张' to '張', '弥' to '彌',
        '弯' to '灣', '弹' to '彈', '强' to '強', '归' to '歸', '当' to '當',
        '录' to '錄', '汇' to '彙', '彦' to '彥', '彩' to '彩', '彭' to '彭',
        '彰' to '彰', '影' to '影', '彻' to '徹', '征' to '徵', '径' to '徑',
        '徕' to '徠', '徊' to '迴', '忆' to '憶', '忏' to '懺', '忧' to '憂',
        '怀' to '懷', '态' to '態', '怂' to '慫', '怃' to '憮', '怅' to '悵',
        '怆' to '愴', '怜' to '憐', '总' to '總', '怼' to '懟', '怿' to '懌',
        '恋' to '戀', '恒' to '恆', '恳' to '懇', '恶' to '惡', '恸' to '慟',
        '恺' to '愷', '恻' to '惻', '恼' to '惱', '恽' to '惲', '涌' to '湧',
        '绝' to '絕', '战' to '戰', '视' to '視', '觉' to '覺', '触' to '觸',
        '说' to '說', '话' to '話', '读' to '讀', '画' to '畫', '统' to '統',
        '设' to '設', '搜' to '搜', '试' to '試', '练' to '練', '选' to '選',
        '续' to '續', '播' to '播', '网' to '網', '络' to '絡', '线' to '線',
        '观' to '觀', '预' to '預', '告' to '告', '连' to '連', '载' to '載',
        '结' to '結', '热' to '熱', '门' to '門', '精' to '精', '荐' to '薦',
        '风' to '風', '龙' to '龍', '剑' to '劍', '灵' to '靈', '飞' to '飛',
        '阳' to '陽', '阴' to '陰', '极' to '極', '汉' to '漢', '语' to '語',
        '组' to '組', '练' to '練', '炼' to '煉', '术' to '術'
    )

    private val charMap: Map<Char, Char> = charPairs.toMap()
    private val reverseCharMap: Map<Char, Char> = charPairs.associate { (simplified, traditional) -> traditional to simplified }

    // Hoisted so hot paths (mapping ~1,900 titles on sync, per-card renders) do
    // not recompile these patterns on every call.
    private val EP_PATTERN = Regex("""(?i)\bEP\s*(\d+)""")
    private val HUA_PATTERN = Regex("""第\s*(\d+)\s*話""")
    private val TRAILING_HUA_PATTERN = Regex("""(\d+)\s*話""")

    fun toSimplifiedChinese(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(reverseCharMap[ch] ?: ch)
        }
        return sb.toString()
    }

    fun toTraditionalChinese(text: String?): String {
        if (text.isNullOrBlank()) return ""

        var result: String = text

        // 1. Convert phrase matches first
        phraseMap.forEach { (simplified, traditional) ->
            if (result.contains(simplified)) {
                result = result.replace(simplified, traditional)
            }
        }

        // 2. Character by character conversion
        val sb = StringBuilder(result.length)
        for (ch in result) {
            sb.append(charMap[ch] ?: ch)
        }
        result = sb.toString()

        // 3. Normalize Episode / Season / HD terminology
        result = result
            .replace(EP_PATTERN, "第 $1 集")
            .replace(HUA_PATTERN, "第 $1 集")
            .replace(TRAILING_HUA_PATTERN, "$1 集")
            .replace("HD 1080P", "高清 1080P")

        return result
    }
}
