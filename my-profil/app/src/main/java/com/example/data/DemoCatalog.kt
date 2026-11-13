package com.example.data

import com.example.R
import com.example.model.Comment
import com.example.model.Post
import com.example.model.PublicProfile
import com.google.firebase.Timestamp
import java.util.Date

object DemoCatalog {
    fun sampleProfiles(language: String): List<PublicProfile> {
        val isAr = language == "ar"
        return listOf(
            PublicProfile(
                id = "demo_salma",
                userId = "demo_salma",
                displayName = if (isAr) "سلمى المنصور" else "Salma Al-Mansoor",
                handle = "salma.studio",
                bio = if (isAr) {
                    "مصممة منتجات وخط عربي معاصر. أشارك يوميات التصميم والقهوة."
                } else {
                    "Product designer & modern Arabic calligrapher. Sharing studio notes & coffee."
                },
                avatarSeed = 1,
                isPrivate = false,
                followersCount = 148,
                followingCount = 62,
                postsCount = 2,
                isDemo = true,
            ),
            PublicProfile(
                id = "demo_tariq",
                userId = "demo_tariq",
                displayName = if (isAr) "طارق الهلالي" else "Tariq Al-Hilali",
                handle = "tariq.travels",
                bio = if (isAr) {
                    "مصور طبيعة ورحّالة. أبحث عن الضوء الذهبي في السواحل والجبال."
                } else {
                    "Landscape photographer & traveler chasing golden hour along coasts and ridges."
                },
                avatarSeed = 2,
                isPrivate = false,
                followersCount = 312,
                followingCount = 94,
                postsCount = 1,
                isDemo = true,
            ),
        )
    }

    fun samplePosts(language: String): List<Post> {
        val isAr = language == "ar"
        val nowMillis = System.currentTimeMillis()
        return listOf(
            Post(
                id = "demo_post_1",
                authorId = "demo_salma",
                authorName = if (isAr) "سلمى المنصور" else "Salma Al-Mansoor",
                authorHandle = "salma.studio",
                authorAvatarSeed = 1,
                content = if (isAr) {
                    "صباح هادئ في الاستوديو قبل بدء مراجعة تصاميم الواجهة الجديدة. التبسيط وإعطاء المساحة للتفاصيل يصنعان فرقًا كبيرًا في تجربة المستخدم."
                } else {
                    "Quiet morning at the studio before reviewing our new interface concepts. Giving details room to breathe always transforms the experience."
                },
                visibility = "public",
                likesCount = 24,
                commentsCount = 2,
                createdAt = Timestamp(Date(nowMillis - 2 * 3600_000L)),
                updatedAt = Timestamp(Date(nowMillis - 2 * 3600_000L)),
                isDemo = true,
                demoDrawableRes = R.drawable.img_demo_post_1,
            ),
            Post(
                id = "demo_post_2",
                authorId = "demo_tariq",
                authorName = if (isAr) "طارق الهلالي" else "Tariq Al-Hilali",
                authorHandle = "tariq.travels",
                authorAvatarSeed = 2,
                content = if (isAr) {
                    "ساعة الغروب على المنحدرات الساحلية اليوم كانت استثنائية. الطبيعة تمنحنا أفضل الألوان دون أي تعديل."
                } else {
                    "Golden hour overlooking the coastal cliffs today was unforgettable. Nature always paints the sharpest palette."
                },
                visibility = "public",
                likesCount = 41,
                commentsCount = 1,
                createdAt = Timestamp(Date(nowMillis - 6 * 3600_000L)),
                updatedAt = Timestamp(Date(nowMillis - 6 * 3600_000L)),
                isDemo = true,
                demoDrawableRes = R.drawable.img_demo_post_2,
            ),
        )
    }

    fun sampleComments(postId: String, language: String): List<Comment> {
        val isAr = language == "ar"
        val nowMillis = System.currentTimeMillis()
        return when (postId) {
            "demo_post_1" -> listOf(
                Comment(
                    id = "demo_c_1",
                    postId = "demo_post_1",
                    authorId = "demo_tariq",
                    authorName = if (isAr) "طارق الهلالي" else "Tariq Al-Hilali",
                    authorAvatarSeed = 2,
                    text = if (isAr) "أجواء رائعة جدًا! بالتوفيق في التصميم الجديد." else "Such a calm workspace! Good luck with the new release.",
                    createdAt = Timestamp(Date(nowMillis - 90 * 60_000L)),
                    isDemo = true,
                ),
                Comment(
                    id = "demo_c_2",
                    postId = "demo_post_1",
                    authorId = "demo_salma",
                    authorName = if (isAr) "سلمى المنصور" else "Salma Al-Mansoor",
                    authorAvatarSeed = 1,
                    text = if (isAr) "شكرًا لك يا طارق!" else "Thank you Tariq!",
                    createdAt = Timestamp(Date(nowMillis - 45 * 60_000L)),
                    isDemo = true,
                ),
            )
            "demo_post_2" -> listOf(
                Comment(
                    id = "demo_c_3",
                    postId = "demo_post_2",
                    authorId = "demo_salma",
                    authorName = if (isAr) "سلمى المنصور" else "Salma Al-Mansoor",
                    authorAvatarSeed = 1,
                    text = if (isAr) "لقطة مدهشة وتناسق ألوان هادئ!" else "Incredible shot and serene color tones!",
                    createdAt = Timestamp(Date(nowMillis - 4 * 3600_000L)),
                    isDemo = true,
                ),
            )
            else -> emptyList()
        }
    }
}
