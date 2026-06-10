package com.nuclearboy.ui.chat.parts

internal fun shouldFollowChatScroll(
    totalItemsCount: Int,
    lastVisibleItemIndex: Int?,
    trailingItemThreshold: Int = 1,
): Boolean {
    if (totalItemsCount <= 0) return true
    if (lastVisibleItemIndex == null) return true
    val lastIndex = totalItemsCount - 1
    return lastIndex - lastVisibleItemIndex <= trailingItemThreshold
}

internal fun shouldShowJumpToBottom(
    totalItemsCount: Int,
    lastVisibleItemIndex: Int?,
    trailingItemThreshold: Int = 1,
): Boolean {
    return totalItemsCount > 0 && !shouldFollowChatScroll(
        totalItemsCount = totalItemsCount,
        lastVisibleItemIndex = lastVisibleItemIndex,
        trailingItemThreshold = trailingItemThreshold,
    )
}
