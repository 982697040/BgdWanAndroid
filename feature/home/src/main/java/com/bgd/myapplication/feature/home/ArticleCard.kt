package com.bgd.myapplication.feature.home

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bgd.myapplication.core.model.Article

@Composable
internal fun ArticleCard(article: Article, pinned: Boolean = false) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Card(
        onClick = {
            try {
                val uri = Uri.parse(article.url)
                require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank())
                uriHandler.openUri(article.url)
            } catch (_: Exception) {
                Toast.makeText(context, R.string.link_error, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).testTag("article_${article.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(start = 10.dp, top = 10.dp, end = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pinned) Text(stringResource(R.string.article_pinned),
                    Modifier.padding(end = 8.dp), color = Color(0xFFFF443D), fontSize = 12.sp)
                Text(article.author, Modifier.weight(1f).padding(end = 8.dp), fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(article.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(article.title, Modifier.padding(top = 12.dp, end = 4.dp),
                fontSize = 14.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(article.category, Modifier.weight(1f), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = {
                    Toast.makeText(context, R.string.collect_login, Toast.LENGTH_SHORT).show()
                }) {
                    Icon(painterResource(R.drawable.ic_star),
                        stringResource(if (article.collected) R.string.collected else R.string.collect),
                        tint = if (article.collected) Color(0xFFE52310) else Color(0xFF999999),
                        modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
