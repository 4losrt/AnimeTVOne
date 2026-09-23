package com.one.animetvone.data.repository

import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.model.AnimeFeedSection
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.model.VideoStream

object SampleAnimeData {

    val sampleAnime1Items = listOf(
        Anime1Item(1, "葬送的芙莉蓮", "28話", "2023年秋", "SweetSub", "TV"),
        Anime1Item(2, "鬼滅之刃 柱訓練篇", "8話", "2024年春", "櫻都", "TV"),
        Anime1Item(3, "咒術迴戰 第二季", "23話", "2023年夏", "喵萌", "TV"),
        Anime1Item(4, "無職轉生～到了異世界就拿出真本事～", "24話", "2023年夏", "動漫國", "TV"),
        Anime1Item(5, "間諜家家酒", "25話", "2022年春", "極影", "TV"),
        Anime1Item(6, "我推的孩子", "11話", "2023年春", "動漫國", "TV"),
        Anime1Item(7, "轉生史萊姆 第三季", "24話", "2024年春", "櫻都", "TV"),
        Anime1Item(8, "怪獸8號", "12話", "2024年春", "極影", "TV"),
        Anime1Item(9, "排球少年", "25話", "2020年秋", "動漫國", "TV"),
        Anime1Item(10, "鏈鋸人", "12話", "2022年秋", "極影", "TV")
    )

    val sampleAnimes = listOf(
        Anime(
            id = "frieren",
            title = "葬送的芙莉蓮",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/7f/81/413803_J7m4Z.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/7f/81/413803_J7m4Z.jpg",
            description = "長生不老的精靈魔法使在勇者一行人討伐魔王後，展開探索生命、友情與時光意義的溫馨冒險。",
            rating = 9.5,
            genres = listOf("冒險", "奇幻", "日常", "2023年秋"),
            year = 2023,
            status = "已完結",
            episodeCount = 28
        ),
        Anime(
            id = "demon_slayer_hashira",
            title = "鬼滅之刃 柱訓練篇",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/d0/22/276591_O2vWw.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/d0/22/276591_O2vWw.jpg",
            description = "少年劍士為了對抗鬼舞辻無慘與鬼殺隊柱們展開嚴酷的特訓，準備迎接決戰。",
            rating = 9.0,
            genres = listOf("熱血", "動作", "奇幻", "2024年春"),
            year = 2024,
            status = "已完結",
            episodeCount = 8
        ),
        Anime(
            id = "jujutsu_kaisen_s2",
            title = "咒術迴戰 第二季",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/8e/ec/295325_Y4vT3.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/8e/ec/295325_Y4vT3.jpg",
            description = "揭開五條悟與夏油傑的懷玉・玉折高專時期故事，以及震撼咒術界與澀谷事變的決戰。",
            rating = 9.2,
            genres = listOf("熱血", "動作", "超自然", "2023年夏"),
            year = 2023,
            status = "已完結",
            episodeCount = 23
        ),
        Anime(
            id = "mushoku_tensei",
            title = "無職轉生～到了異世界就拿出真本事～",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/ca/27/292671_YFmFf.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/ca/27/292671_YFmFf.jpg",
            description = "34歲尼特族在轉生到劍與魔法的異世界後，決定以魯迪烏斯的名義認真度過新的人生。",
            rating = 9.1,
            genres = listOf("冒險", "奇幻", "異世界", "2023年夏"),
            year = 2023,
            status = "已完結",
            episodeCount = 24
        ),
        Anime(
            id = "spy_family",
            title = "間諜家家酒",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/bf/3e/355343_8A84c.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/bf/3e/355343_8A84c.jpg",
            description = "頂尖間諜為了執行機密任務組建臨時家庭，養女擁有讀心超能力，妻子竟是頂級殺手。",
            rating = 8.8,
            genres = listOf("喜劇", "動作", "日常", "2022年春"),
            year = 2022,
            status = "已完結",
            episodeCount = 25
        ),
        Anime(
            id = "oshi_no_ko",
            title = "我推的孩子",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/1d/1b/388053_8jZzU.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/1d/1b/388053_8jZzU.jpg",
            description = "演藝圈背後的秘密與謊言，轉生為傳奇偶像星野愛的孩子，揭開娛樂圈真實的面貌。",
            rating = 9.3,
            genres = listOf("劇情", "懸疑", "偶像", "2023年春"),
            year = 2023,
            status = "已完結",
            episodeCount = 11
        ),
        Anime(
            id = "slime_s3",
            title = "轉生史萊姆 第三季",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/fb/20/240360_oF9Yd.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/fb/20/240360_oF9Yd.jpg",
            description = "利姆路建立的魔國聯邦持續發展，面對神聖法皇國與開國祭的挑戰。",
            rating = 8.9,
            genres = listOf("冒險", "奇幻", "異世界", "2024年春"),
            year = 2024,
            status = "連載中",
            episodeCount = 24
        ),
        Anime(
            id = "kaiju_no8",
            title = "怪獸8號",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/b8/91/394747_A4aO6.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/b8/91/394747_A4aO6.jpg",
            description = "在怪獸頻繁出現的日本，負責清理怪獸屍體的日比野卡夫卡意外獲得化身怪獸的能力。",
            rating = 8.7,
            genres = listOf("熱血", "動作", "科幻", "2024年春"),
            year = 2024,
            status = "已完結",
            episodeCount = 12
        ),
        Anime(
            id = "haikyuu",
            title = "排球少年",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/08/9d/86435_lZp2k.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/08/9d/86435_lZp2k.jpg",
            description = "日向翔陽與影山飛雄在烏野高中排球部相遇，為了挺進全國大賽而並肩作戰。",
            rating = 9.4,
            genres = listOf("熱血", "運動", "校園", "2020年秋"),
            year = 2020,
            status = "已完結",
            episodeCount = 25
        ),
        Anime(
            id = "chainsaw_man",
            title = "鏈鋸人",
            coverUrl = "https://lain.bgm.tv/r/400/pic/cover/l/41/39/322883_2XJ9c.jpg",
            bannerUrl = "https://lain.bgm.tv/r/400/pic/cover/l/41/39/322883_2XJ9c.jpg",
            description = "少年淀治與鏈鋸惡魔波奇塔簽訂契約，重生成為鏈鋸人並加入公安對魔特異課。",
            rating = 8.9,
            genres = listOf("熱血", "動作", "超自然", "2022年秋"),
            year = 2022,
            status = "已完結",
            episodeCount = 12
        )
    )

    private val sampleStreams = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
    )

    val sampleEpisodesMap: Map<String, List<Episode>> = sampleAnimes.associate { anime ->
        anime.id to List(anime.episodeCount.coerceAtMost(12)) { index ->
            val epNum = index + 1
            val streamUrl = sampleStreams[(index + anime.id.hashCode().coerceAtLeast(0)) % sampleStreams.size]
            Episode(
                id = "${anime.id}_ep_$epNum",
                animeId = anime.id,
                episodeNumber = epNum,
                title = "第 $epNum 集：${getEpisodeTitle(anime.id, epNum)}",
                thumbnailUrl = anime.bannerUrl,
                duration = "24:00",
                streamUrl = streamUrl
            )
        }
    }

    private fun getEpisodeTitle(animeId: String, number: Int): String {
        return when (number) {
            1 -> "覺醒之刻"
            2 -> "過往的回響"
            3 -> "陰影策略"
            4 -> "不熄的炎之意志"
            5 -> "外圍之門"
            6 -> "破碎的地平線"
            7 -> "無聲安魂曲"
            8 -> "緋紅之刃"
            9 -> "風暴再起"
            10 -> "決死對決"
            11 -> "最終界限"
            12 -> "命運的晨曦"
            else -> "第 $number 章"
        }
    }

    val sampleFeedSections: List<AnimeFeedSection> = listOf(
        AnimeFeedSection(
            id = "featured",
            title = "精選推薦",
            animes = listOf(sampleAnimes[0], sampleAnimes[2], sampleAnimes[4], sampleAnimes[1])
        ),
        AnimeFeedSection(
            id = "trending",
            title = "熱門連載",
            animes = listOf(sampleAnimes[3], sampleAnimes[0], sampleAnimes[5], sampleAnimes[6])
        ),
        AnimeFeedSection(
            id = "recent",
            title = "今日更新",
            animes = listOf(sampleAnimes[2], sampleAnimes[1], sampleAnimes[7], sampleAnimes[3])
        ),
        AnimeFeedSection(
            id = "popular",
            title = "人氣熱播",
            animes = listOf(sampleAnimes[4], sampleAnimes[6], sampleAnimes[0], sampleAnimes[2], sampleAnimes[5])
        )
    )

    fun getVideoStreamForEpisode(episodeId: String): VideoStream {
        val episode = sampleEpisodesMap.values.flatten().find { it.id == episodeId }
        val streamUrl = episode?.streamUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        return VideoStream(
            url = streamUrl,
            quality = "高清 1080P",
            serverName = "極速分流伺服器"
        )
    }
}
