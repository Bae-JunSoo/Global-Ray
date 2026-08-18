// 아이디 중복 확인 완료 여부 — 확인 없이 폼 제출 방지
let idChecked = false;

/* ===== 아이디 중복 확인 ===== */
async function checkId() {
    const userId = document.getElementById('userId').value.trim();
    const msg    = document.getElementById('idMsg');

    if (!userId) { showMsg(msg, '아이디를 입력해주세요.', false); return; }
    if (userId.length < 4 || userId.length > 20) {
        showMsg(msg, '아이디는 4~20자리로 입력해주세요.', false); return;
    }

    idChecked = false;
    const res  = await fetch(`/auth/api/check-id?userId=${encodeURIComponent(userId)}`);
    const data = await res.json();

    if (data.duplicate) {
        showMsg(msg, '이미 사용 중인 아이디입니다.', false);
    } else {
        showMsg(msg, '사용 가능한 아이디입니다.', true);
        idChecked = true;
    }
}

/* ===== 이메일 인증코드 발송 ===== */
async function sendAuthCode() {
    const email = document.getElementById('userEmail').value.trim();
    const msg   = document.getElementById('codeMsg');
    const btn   = document.getElementById('sendCodeBtn');

    if (!email) { showMsg(msg, '이메일을 입력해주세요.', false); return; }

    // 중복 요청 방지: 발송 중 버튼 비활성화
    btn.disabled    = true;
    btn.textContent = '발송 중...';

    try {
        const res  = await fetch('/auth/api/send-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
            body: JSON.stringify({ email })
        });
        const data = await res.json();
        showMsg(msg, data.message, res.ok);
    } finally {
        btn.disabled    = false;
        btn.textContent = '전송';
    }
}

/* ===== 이메일 인증코드 검증 ===== */
async function verifyCode() {
    const email = document.getElementById('userEmail').value.trim();
    const code  = document.getElementById('authCode').value.trim();
    const msg   = document.getElementById('codeMsg');
    const btn   = document.getElementById('verifyCodeBtn');

    if (!email || !code) {
        showMsg(msg, '이메일과 인증코드를 입력해주세요.', false); return;
    }

    // 중복 요청 방지
    btn.disabled    = true;
    btn.textContent = '확인 중...';

    try {
        const res  = await fetch('/auth/api/verify-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
            body: JSON.stringify({ email, code })
        });
        const data = await res.json();

        if (data.verified) {
            showMsg(msg, '이메일 인증이 완료되었습니다.', true);
            document.getElementById('submitBtn').disabled = false;
        } else {
            showMsg(msg, '인증코드가 올바르지 않거나 만료되었습니다.', false);
        }
    } finally {
        btn.disabled    = false;
        btn.textContent = '확인';
    }
}

/* ===== 폼 제출 이벤트 (onsubmit 인라인 핸들러 대체) ===== */
// main.js 의 showAlert 을 사용하므로 main.js 를 먼저 로드해야 함
document.getElementById('signupForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    if (!idChecked) {
        await showAlert('아이디 중복 확인을 해주세요.');
        return;
    }

    const pw  = document.getElementById('userPw').value;
    const cpw = document.getElementById('confirmPw').value;

    // 비밀번호 길이: SignupRequest @Size(min=8) 과 일치
    if (pw.length < 8) {
        await showAlert('비밀번호는 8자 이상 입력해주세요.');
        return;
    }
    if (pw !== cpw) {
        await showAlert('비밀번호가 일치하지 않습니다.');
        return;
    }

    this.submit();
});

/* ===== 버튼 이벤트 연결 (onclick 인라인 제거 후 여기서 일괄 바인딩) ===== */
document.getElementById('checkIdBtn').addEventListener('click', checkId);
document.getElementById('sendCodeBtn').addEventListener('click', sendAuthCode);
document.getElementById('verifyCodeBtn').addEventListener('click', verifyCode);

/* ===== 메시지 표시 유틸 ===== */
function showMsg(el, text, isOk) {
    el.textContent = text;
    el.className   = 'field-msg ' + (isOk ? 'ok' : 'err');
}
