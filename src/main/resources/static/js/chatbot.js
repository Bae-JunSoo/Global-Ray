/* ===== DOM 요소 ===== */
const chatMessages    = document.getElementById('chatMessages');
const questionInput   = document.getElementById('questionInput');
const sendBtn         = document.getElementById('sendBtn');
const clearHistoryBtn = document.getElementById('clearHistoryBtn');

sendBtn.addEventListener('click', sendQuestion);
clearHistoryBtn.addEventListener('click', clearHistory);

document.querySelectorAll('[data-action="fill-question"]').forEach(btn => {
    btn.addEventListener('click', () => fillQuestion(btn.dataset.question));
});

loadHistory();

/* ===== 이전 대화 불러오기 ===== */
async function loadHistory() {
    try {
        const res = await fetch('/chatbot/history');
        if (!res.ok) return;
        const history = await res.json();
        history.forEach(h => {
            appendMsg('user', h.question);
            appendMsg('bot', h.answer);
        });
    } catch (e) {
        // 이력 조회 실패는 채팅 사용에 영향이 없으므로 무시
    }
}

/* ===== 질문 전송 ===== */
async function sendQuestion() {
    const question = questionInput.value.trim();
    if (!question) return;

    appendMsg('user', question);
    questionInput.value = '';
    sendBtn.disabled = true;

    const typingEl = appendTyping();

    try {
        const res = await fetch('/chatbot/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
            body: JSON.stringify({ question })
        });

        const data = await res.json();
        typingEl.remove();

        if (!res.ok) {
            appendMsg('bot', data.message || '오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            return;
        }

        const botEl = appendMsg('bot', data.answer);
        appendSources(botEl, data.sources);

    } catch (e) {
        typingEl.remove();
        appendMsg('bot', '오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
        sendBtn.disabled = false;
        questionInput.focus();
    }
}

/* ===== 대화 기록 삭제 ===== */
async function clearHistory() {
    if (!confirm('저장된 대화 기록을 모두 삭제할까요?')) return;

    const res = await fetch('/chatbot/history', {
        method: 'DELETE',
        headers: { ...getCsrfHeaders() }
    });

    if (res.ok) {
        chatMessages.querySelectorAll('.msg:not(.welcome)').forEach(el => el.remove());
    } else {
        alert('대화 기록을 삭제하지 못했습니다.');
    }
}

/* ===== 메시지 추가 ===== */
function appendMsg(role, text) {
    const div = document.createElement('div');
    div.className = `msg ${role}`;
    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    bubble.textContent = text;
    div.appendChild(bubble);
    chatMessages.appendChild(div);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return div;
}

/* ===== 답변 근거 기사 링크 ===== */
function appendSources(msgEl, sources) {
    if (!sources || sources.length === 0) return;

    const box = document.createElement('div');
    box.className = 'chat-sources';
    sources.forEach((s, i) => {
        const a = document.createElement('a');
        a.href = `/news/detail/${encodeURIComponent(s.articleId)}`;
        a.target = '_blank';
        a.textContent = `[${i + 1}] ${s.title}${s.sourceName ? ' · ' + s.sourceName : ''}`;
        box.appendChild(a);
    });
    msgEl.querySelector('.bubble').appendChild(box);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

/* ===== 로딩 점 표시 ===== */
function appendTyping() {
    const div = document.createElement('div');
    div.className = 'msg bot typing';
    div.innerHTML = `
    <div class="bubble">
      <span class="dot"></span>
      <span class="dot"></span>
      <span class="dot"></span>
    </div>`;
    chatMessages.appendChild(div);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return div;
}

function fillQuestion(text) {
    questionInput.value = text;
    questionInput.focus();
}

/* ===== Enter 전송, Shift+Enter 줄바꿈 ===== */
questionInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendQuestion();
    }
});
