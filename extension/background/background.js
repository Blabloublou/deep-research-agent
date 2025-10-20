// Background service worker for Deep Research Agent
// Opens the side panel when the extension icon is clicked

chrome.action.onClicked.addListener((tab) => {
  // Open the side panel for the current tab
  chrome.sidePanel.open({ tabId: tab.id });
});

// Optional: Set side panel behavior to be available on all sites
chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true }).catch((error) => {
  console.error(error);
});

