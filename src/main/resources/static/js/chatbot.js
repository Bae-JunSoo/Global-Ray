/* ===== DOM 요소 ===== */
const chatMessages  = document.getElementById('chatMessages');
const questionInput = document.getElementById('questionInput');
const sendBtn       = document.getElementById('sendBtn');

/* ===== 전송 버튼 이벤트 (onclick 인라인 핸들러 대체) ===== */
sendBtn.addEventListener('click', sendQuestion);

/* ===== 예시 질문 버튼 이벤트 위임 ===== */
// data-action="fill-question" + data-question="..." 속성으로 처리
document.querySelectorAll('[data-action="fill-question"]').forEach(btn => {
    btn.addEventListener('click', () => fillQuestion(btn.dataset.question));
});

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
        appendMsg('bot', data.answer || '답변을 가져오지 못했습니다.');

    } catch (e) {
        typingEl.remove();
        appendMsg('bot', '오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
        sendBtn.disabled = false;
        questionInput.focus();
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

/* ===== 로딩 점 표시 (봇이 답변 중일 때) ===== */
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

/* ===== 예시 질문 입력창에 채우기 ===== */
function fillQuestion(text) {
    questionInput.value = text;
    questionInput.focus();
}

/* ===== Enter 키로 전송 (Shift+Enter는 줄바꿈) ===== */
questionInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendQuestion();
    }
});
