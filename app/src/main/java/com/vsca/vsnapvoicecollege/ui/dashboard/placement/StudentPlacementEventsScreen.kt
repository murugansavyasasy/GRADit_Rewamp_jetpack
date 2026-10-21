package com.vsca.vsnapvoicecollege.ui.dashboard.placement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vsca.vsnapvoicecollege.R
import com.vsca.vsnapvoicecollege.ui.components.OfflineBanner
import com.vsca.vsnapvoicecollege.ui.components.SearchBar
import com.vsca.vsnapvoicecollege.ui.components.SkeletonCards
import com.vsca.vsnapvoicecollege.ui.components.StateMessage
import com.vsca.vsnapvoicecollege.ui.components.TitledTopBar
import com.vsca.vsnapvoicecollege.ui.components.clockLabel
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.MessageCard
import com.vsca.vsnapvoicecollege.ui.dashboard.notices.RetryButton
import com.vsca.vsnapvoicecollege.ui.theme.AlertBackground
import com.vsca.vsnapvoicecollege.ui.theme.AppTextSize
import com.vsca.vsnapvoicecollege.ui.theme.BadgeRed
import com.vsca.vsnapvoicecollege.ui.theme.BrandBlue
import com.vsca.vsnapvoicecollege.ui.theme.FieldBorder
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme
import com.vsca.vsnapvoicecollege.ui.theme.ScreenBackground
import com.vsca.vsnapvoicecollege.ui.theme.SelectedRowBackground
import com.vsca.vsnapvoicecollege.ui.theme.TextPrimary
import com.vsca.vsnapvoicecollege.ui.theme.TextSecondary
import com.vsca.vsnapvoicecollege.ui.theme.White
import java.util.Locale

/** The student's list of upcoming placement events; a row opens the event. */
@Composable
fun StudentPlacementEventsScreen(
    onBack: () -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentPlacementViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentPlacementContent(uiState, onBack, viewModel::onQueryChange, viewModel::refresh, onOpenEvent, modifier)
}

@Composable
private fun StudentPlacementContent(
    uiState: StudentPlacementUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { TitledTopBar(title = stringResource(R.string.placement_title), onBack = onBack) }
        if (!uiState.isOnline && uiState.load != PlacementLoad.ERROR) {
            item(key = "offline") {
                OfflineBanner(
                    stringResource(R.string.placement_offline_banner, syncedLabel(uiState.lastSyncedAt)),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        item(key = "search") {
            SearchBar(
                query = uiState.query,
                onQueryChange = onQuery,
                hint = stringResource(R.string.placement_student_search_hint),
                containerColor = White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp)),
            )
        }
        when {
            uiState.load == PlacementLoad.LOADING -> item(key = "loading") { SkeletonCards(2, Modifier.padding(16.dp)) }
            uiState.load == PlacementLoad.ERROR -> item(key = "error") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_wifi,
                        iconTint = BadgeRed,
                        iconBackground = AlertBackground,
                        title = stringResource(R.string.placement_error_title),
                        subtitle = stringResource(R.string.placement_error_body),
                    ) { RetryButton(onRetry) }
                }
            }
            uiState.events.isEmpty() -> item(key = "empty") {
                MessageCard {
                    StateMessage(
                        iconRes = R.drawable.ic_work,
                        iconTint = BrandBlue,
                        iconBackground = SelectedRowBackground,
                        title = stringResource(if (uiState.query.isBlank()) R.string.placement_none_upcoming_title else R.string.placement_no_match_title),
                        subtitle = stringResource(if (uiState.query.isBlank()) R.string.placement_none_body else R.string.placement_student_no_match_body),
                    )
                }
            }
            else -> item(key = "events") {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = White,
                    border = BorderStroke(1.dp, FieldBorder),
                ) {
                    Column {
                        uiState.events.forEachIndexed { index, event ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = FieldBorder)
                            StudentEventRow(event, onClick = { onOpenEvent(event.id) })
                        }
                    }
                }
            }
        }
    }
}

/** "Zoho · campus drive / Software developer · ₹8.4 LPA · Main auditorium / 14 Oct". */
@Composable
private fun StudentEventRow(event: PlacementEvent, onClick: () -> Unit) {
    val type = stringResource(event.type.label).lowercase(Locale.getDefault())
    val title = event.companies.singleOrNull()?.let { stringResource(R.string.placement_student_row_title, it.name, type) } ?: event.title
    val details = listOfNotNull(event.offer, event.venue, if (event.offer == null) clockLabel(event.startAt) else null).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(SelectedRowBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_work), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = AppTextSize.BodyLarge, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
            Spacer(Modifier.height(2.dp))
            Text(details, color = TextSecondary, fontSize = AppTextSize.Label, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text(shortDate(event.startAt), color = TextPrimary, fontSize = AppTextSize.Label, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StudentPlacementPreview() {
    GRADit_RewampTheme {
        val now = System.currentTimeMillis()
        StudentPlacementContent(
            uiState = StudentPlacementUiState(
                load = PlacementLoad.READY,
                events = PlacementRepository.events.value.filterNot { it.hasEnded(now) }.sortedBy { it.startAt },
            ),
            onBack = {}, onQuery = {}, onRetry = {}, onOpenEvent = {},
        )
    }
}
