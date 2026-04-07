// ============================================================
// Client Outreach System — Mihir Botle
// ============================================================

// --- Tab switching ---
function showTab(tabName, btn) {
  document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
  const tab = document.getElementById('tab-' + tabName);
  if (tab) tab.classList.add('active');
  if (btn) btn.classList.add('active');
}

// --- Copy text ---
function copyText(elementId) {
  const el = document.getElementById(elementId);
  if (!el) return;

  const text = el.innerText || el.textContent;
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      showCopied();
    }).catch(() => {
      fallbackCopy(text);
    });
  } else {
    fallbackCopy(text);
  }
}

function fallbackCopy(text) {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  try {
    document.execCommand('copy');
    showCopied();
  } catch(e) {}
  document.body.removeChild(ta);
}

function showCopied() {
  const el = document.createElement('div');
  el.textContent = 'Copied!';
  el.style.cssText = `
    position: fixed; bottom: 24px; right: 24px;
    background: #4ade80; color: #0f1117;
    padding: 8px 18px; border-radius: 8px;
    font-size: 13px; font-weight: 600;
    z-index: 9999; animation: fadeIn 0.2s;
  `;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 1800);
}

// --- Search form loading state ---
const searchForm = document.getElementById('searchForm');
const searchBtn = document.getElementById('searchBtn');
if (searchForm && searchBtn) {
  searchForm.addEventListener('submit', () => {
    searchBtn.textContent = 'Searching…';
    searchBtn.disabled = true;
  });
}

// --- Auto-dismiss flash messages after 5 seconds ---
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.flash').forEach(el => {
    setTimeout(() => {
      el.style.transition = 'opacity 0.4s';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 400);
    }, 5000);
  });
});
