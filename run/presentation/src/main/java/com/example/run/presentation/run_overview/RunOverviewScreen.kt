@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.run.presentation.run_overview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.core.presentation.designsystem.AnalyticsIcon
import com.example.core.presentation.designsystem.LogoIcon
import com.example.core.presentation.designsystem.LogoutIcon
import com.example.core.presentation.designsystem.RunIcon
import com.example.core.presentation.designsystem.components.RuniqueFloatingActionButton
import com.example.core.presentation.designsystem.components.RuniqueScaffold
import com.example.core.presentation.designsystem.components.RuniqueToolbar
import com.example.core.presentation.designsystem.components.util.DropdownItem
import com.example.run.presentation.R
import com.example.run.presentation.run_overview.components.RunListItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun RunOverviewScreenRoot(
    viewModel: RunOverviewViewModel = koinViewModel(),
    onStartRunClick: () -> Unit
) {
    RunOverviewScreen(
        state = viewModel.state,
        onAction = { action ->
            when (action) {
                RunOverviewAction.OnStartClick -> onStartRunClick()
                else -> Unit
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun RunOverviewScreen(
    state: RunOverviewState,
    onAction: (RunOverviewAction) -> Unit
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)
    RuniqueScaffold(
        topAppBar = {
            RuniqueToolbar(
                title = stringResource(id = R.string.runique),
                showBackButton = false,
                scrollBehavior = scrollBehavior,
                startContent = {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        imageVector = LogoIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                menuItems = listOf(
                    DropdownItem(icon = AnalyticsIcon, title = stringResource(id = R.string.analytics)),
                    DropdownItem(icon = LogoutIcon, title = stringResource(id = R.string.logout))
                ),
                onMenuItemClick = { index ->
                    when (index) {
                        0 -> onAction(RunOverviewAction.OnAnalyticsClick)
                        1 -> onAction(RunOverviewAction.OnLogoutClick)
                    }
                }
            )
        },
        floatingActionButton = {
            RuniqueFloatingActionButton(
                icon = RunIcon,
                onClick = { onAction(RunOverviewAction.OnStartClick) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 16.dp),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = state.runs,
                key = { it.id }
            ) {
                RunListItem(
                    modifier = Modifier.animateItemPlacement(),
                    runUi = it,
                    onDeleteClick = { onAction(RunOverviewAction.DeleteRun(it)) }
                )
            }
        }
    }
}