package com.one.animetvone.data.source

object PrepopulatedCovers {

    private val PREPOPULATED_MAP = mapOf(
        "葬送的芙莉蓮" to "https://lain.bgm.tv/pic/cover/l/7f/81/413803_J7m4Z.jpg",
        "芙莉蓮" to "https://lain.bgm.tv/pic/cover/l/7f/81/413803_J7m4Z.jpg",
        "鬼滅之刃" to "https://lain.bgm.tv/pic/cover/l/d0/22/276591_O2vWw.jpg",
        "咒術迴戰" to "https://lain.bgm.tv/pic/cover/l/8e/ec/295325_Y4vT3.jpg",
        "無職轉生" to "https://lain.bgm.tv/pic/cover/l/ca/27/292671_YFmFf.jpg",
        "間諜家家酒" to "https://lain.bgm.tv/pic/cover/l/bf/3e/355343_8A84c.jpg",
        "間諜過家家" to "https://lain.bgm.tv/pic/cover/l/bf/3e/355343_8A84c.jpg",
        "鏈鋸人" to "https://lain.bgm.tv/pic/cover/l/41/39/322883_2XJ9c.jpg",
        "電鋸人" to "https://lain.bgm.tv/pic/cover/l/41/39/322883_2XJ9c.jpg",
        "我推的孩子" to "https://lain.bgm.tv/pic/cover/l/1d/1b/388053_8jZzU.jpg",
        "轉生史萊姆" to "https://lain.bgm.tv/pic/cover/l/fb/20/240360_oF9Yd.jpg",
        "關於我轉生變成史萊姆這檔事" to "https://lain.bgm.tv/pic/cover/l/fb/20/240360_oF9Yd.jpg",
        "排球少年" to "https://lain.bgm.tv/pic/cover/l/08/9d/86435_lZp2k.jpg",
        "怪獸8號" to "https://lain.bgm.tv/pic/cover/l/b8/91/394747_A4aO6.jpg",
        "怪獸８號" to "https://lain.bgm.tv/pic/cover/l/b8/91/394747_A4aO6.jpg",
        "進擊的巨人" to "https://lain.bgm.tv/pic/cover/l/16/e0/72895_x6b6Z.jpg",
        "孤獨搖滾" to "https://lain.bgm.tv/pic/cover/l/fb/be/326844_41XkO.jpg",
        "影之強者" to "https://lain.bgm.tv/pic/cover/l/87/49/329712_89U8I.jpg",
        "想要成為影之強者" to "https://lain.bgm.tv/pic/cover/l/87/49/329712_89U8I.jpg",
        "藥師少女的獨語" to "https://lain.bgm.tv/pic/cover/l/34/00/421453_C8C24.jpg",
        "名偵探柯南" to "https://lain.bgm.tv/pic/cover/l/0a/66/899_rI8x8.jpg",
        "柯南" to "https://lain.bgm.tv/pic/cover/l/0a/66/899_rI8x8.jpg",
        "航海王" to "https://lain.bgm.tv/pic/cover/l/bc/fa/1000_18i8z.jpg",
        "海賊王" to "https://lain.bgm.tv/pic/cover/l/bc/fa/1000_18i8z.jpg",
        "死神" to "https://lain.bgm.tv/pic/cover/l/42/48/360216_56C71.jpg",
        "火影忍者" to "https://lain.bgm.tv/pic/cover/l/10/72/840_9y1J1.jpg",
        "火影" to "https://lain.bgm.tv/pic/cover/l/10/72/840_9y1J1.jpg",
        "水星的魔女" to "https://lain.bgm.tv/pic/cover/l/68/73/351609_q0fE8.jpg",
        "機動戰士鋼彈" to "https://lain.bgm.tv/pic/cover/l/68/73/351609_q0fE8.jpg",
        "刀劍神域" to "https://lain.bgm.tv/pic/cover/l/3f/11/27076_I3U6F.jpg",
        "鋼之鍊金術師" to "https://lain.bgm.tv/pic/cover/l/09/b8/1761_o7x5X.jpg",
        "我的英雄學院" to "https://lain.bgm.tv/pic/cover/l/d0/0d/156960_6l9f5.jpg",
        "藍色監獄" to "https://lain.bgm.tv/pic/cover/l/9b/6c/345431_P88P9.jpg",
        "齊木楠雄" to "https://lain.bgm.tv/pic/cover/l/f3/0a/181744_Pq7k5.jpg",
        "齊木楠雄的災難" to "https://lain.bgm.tv/pic/cover/l/f3/0a/181744_Pq7k5.jpg",
        "賽馬娘" to "https://lain.bgm.tv/pic/cover/l/8e/3c/176606_8v33e.jpg",
        "從零開始的異世界生活" to "https://lain.bgm.tv/pic/cover/l/2a/39/140224_c9m6M.jpg",
        "Re:從零開始" to "https://lain.bgm.tv/pic/cover/l/2a/39/140224_c9m6M.jpg",
        "哥布林殺手" to "https://lain.bgm.tv/pic/cover/l/68/e4/238059_vSssK.jpg",
        "不死者之王" to "https://lain.bgm.tv/pic/cover/l/ef/e7/127116_P46c9.jpg",
        "為美好的世界獻上祝福" to "https://lain.bgm.tv/pic/cover/l/8e/20/132801_f1tTF.jpg",
        "86" to "https://lain.bgm.tv/pic/cover/l/08/94/301546_L5S99.jpg",
        "不存在的戰區" to "https://lain.bgm.tv/pic/cover/l/08/94/301546_L5S99.jpg",
        "莉可麗絲" to "https://lain.bgm.tv/pic/cover/l/36/f6/364218_2pXG2.jpg",
        "一拳超人" to "https://lain.bgm.tv/pic/cover/l/10/7a/126936_e814B.jpg",
        "獵人" to "https://lain.bgm.tv/pic/cover/l/d5/d3/20993_P07eY.jpg",
        "七龍珠" to "https://lain.bgm.tv/pic/cover/l/0c/33/2767_M7m7b.jpg",
        "灌籃高手" to "https://lain.bgm.tv/pic/cover/l/e1/9f/3168_K8N62.jpg",
        "網球王子" to "https://lain.bgm.tv/pic/cover/l/45/30/3143_R554E.jpg",
        "哆啦A夢" to "https://lain.bgm.tv/pic/cover/l/13/2a/678_R6Lp2.jpg",
        "銀魂" to "https://lain.bgm.tv/pic/cover/l/34/01/838_0iQ31.jpg",
        "黑色五葉草" to "https://lain.bgm.tv/pic/cover/l/09/27/203496_x6m22.jpg",
        "文豪野犬" to "https://lain.bgm.tv/pic/cover/l/8f/33/145892_f60q8.jpg",
        "東京復仇者" to "https://lain.bgm.tv/pic/cover/l/bd/56/308945_RInTq.jpg",
        "靈能百分百" to "https://lain.bgm.tv/pic/cover/l/87/a2/160867_qX4p4.jpg",
        "輪迴七次的惡役千金" to "https://lain.bgm.tv/pic/cover/l/56/f9/450849_4A58M.jpg",
        "狼與香料" to "https://lain.bgm.tv/pic/cover/l/13/9e/443681_1lI5d.jpg",
        "魔法使的新娘" to "https://lain.bgm.tv/pic/cover/l/7f/7a/206588_m002W.jpg",
        "命運石之門" to "https://lain.bgm.tv/pic/cover/l/a0/0b/8682_1i5K1.jpg",
        "超電磁砲" to "https://lain.bgm.tv/pic/cover/l/c4/3f/2536_976Rk.jpg",
        "魔法禁書目錄" to "https://lain.bgm.tv/pic/cover/l/05/23/1647_a1b2c.jpg",
        "迷宮飯" to "https://lain.bgm.tv/pic/cover/l/54/2e/395345_bNl2m.jpg",
        "蔚藍檔案" to "https://lain.bgm.tv/pic/cover/l/3d/8b/414327_a82Bq.jpg",
        "敗北女角太多了" to "https://lain.bgm.tv/pic/cover/l/a1/38/468239_b72Jq.jpg",
        "鹿乃子" to "https://lain.bgm.tv/pic/cover/l/5d/3c/488235_N6032.jpg",
        "香格里拉" to "https://lain.bgm.tv/pic/cover/l/9c/04/382098_eO24I.jpg",
        "盾之勇者" to "https://lain.bgm.tv/pic/cover/l/97/81/217031_sXN9j.jpg",
        "輝夜姬想讓人告白" to "https://lain.bgm.tv/pic/cover/l/f1/2e/247571_I43kU.jpg",
        "五等分的新娘" to "https://lain.bgm.tv/pic/cover/l/6e/0d/263305_5sF2E.jpg",
        "青春豬頭少年" to "https://lain.bgm.tv/pic/cover/l/3a/05/238128_N889i.jpg",
        "搖曳露營" to "https://lain.bgm.tv/pic/cover/l/e4/20/203328_NOn5F.jpg",
        "輕音少女" to "https://lain.bgm.tv/pic/cover/l/7f/22/1344_l79kX.jpg",
        "花織" to "https://lain.bgm.tv/pic/cover/l/9f/8e/468235_2J8q0.jpg",
        "花織即使是轉生也想打架" to "https://lain.bgm.tv/pic/cover/l/9f/8e/468235_2J8q0.jpg",
        "雅特麗" to "https://lain.bgm.tv/pic/cover/l/42/1b/402835_e002q.jpg",
        "ATRI" to "https://lain.bgm.tv/pic/cover/l/42/1b/402835_e002q.jpg",
        "異世界自殺小隊" to "https://lain.bgm.tv/pic/cover/l/8e/02/443210_R4c2e.jpg",
        "義妹生活" to "https://lain.bgm.tv/pic/cover/l/3d/8c/442981_m513X.jpg",
        "2.5次元的誘惑" to "https://lain.bgm.tv/pic/cover/l/9c/0d/408235_x4X8J.jpg",
        "神之塔" to "https://lain.bgm.tv/pic/cover/l/a0/03/299832_1I6iL.jpg",
        "小市民" to "https://lain.bgm.tv/pic/cover/l/23/8a/473821_B2m3X.jpg",
        "魔王2099" to "https://lain.bgm.tv/pic/cover/l/0e/2b/428235_o0484.jpg",
        "地。-關於地球的運動-" to "https://lain.bgm.tv/pic/cover/l/7f/2b/388235_a1B4c.jpg",
        "關於地球的運動" to "https://lain.bgm.tv/pic/cover/l/7f/2b/388235_a1B4c.jpg",
        "遭到流放的轉生重騎士" to "https://lain.bgm.tv/pic/cover/l/7f/41/471012_A8Z03.jpg",
        "遭到流放的轉生重騎士憑藉遊戲知識大開無雙" to "https://lain.bgm.tv/pic/cover/l/7f/41/471012_A8Z03.jpg",
        "轉生重騎士" to "https://lain.bgm.tv/pic/cover/l/7f/41/471012_A8Z03.jpg"
    )

    fun getPrepopulatedCover(cleanTitle: String): String? {
        val trimmed = cleanTitle.trim()
        if (trimmed.isBlank()) return null

        PREPOPULATED_MAP[trimmed]?.let { return it }

        for ((key, value) in PREPOPULATED_MAP) {
            if (trimmed.contains(key, ignoreCase = true) || key.contains(trimmed, ignoreCase = true)) {
                return value
            }
        }

        return null
    }
}
