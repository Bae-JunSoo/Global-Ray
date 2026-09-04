/* ===== 유저 메뉴 드롭다운 토글 ===== */
function toggleUserMenu() {
    const dropdown = document.getElementById('userDropdown');
    if (dropdown) dropdown.classList.toggle('open');
}

/* ===== 커스텀 모달 (native alert/confirm 대체) ===== */
// native alert()·confirm() 은 UI를 완전히 차단하고 브라우저마다 스타일이 다름
// Promise 반환 → async/await 코드에서 await showAlert() 로 자연스럽게 사용

function showAlert(message) {
    return new Promise(resolve => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
            <div class="modal-box">
                <p class="modal-msg">${message}</p>
                <div class="modal-actions">
                    <button class="btn-primary modal-ok">확인</button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        overlay.querySelector('.modal-ok').addEventListener('click', () => {
            overlay.remove();
            resolve();
        });
    });
}

function showConfirm(message) {
    return new Promise(resolve => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
            <div class="modal-box">
                <p class="modal-msg">${message}</p>
                <div class="modal-actions">
                    <button class="btn-primary modal-ok">확인</button>
                    <button class="btn-secondary modal-cancel">취소</button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        overlay.querySelector('.modal-ok').addEventListener('click', () => {
            overlay.remove();
            resolve(true);
        });
        overlay.querySelector('.modal-cancel').addEventListener('click', () => {
            overlay.remove();
            resolve(false);
        });
    });
}

/* ===== 이벤트 위임 ===== */
// 인라인 onclick 대신 data-action 속성으로 의도를 명시하고 여기서 일괄 처리
// 장점: HTML에 JS가 섞이지 않음, 동적으로 추가된 요소도 자동 처리
document.addEventListener('click', function(e) {
    // 드롭다운 외부 클릭 시 닫기
    const wrap = document.querySelector('.user-menu-wrap');
    if (wrap && !wrap.contains(e.target)) {
        document.getElementById('userDropdown')?.classList.remove('open');
    }

    // data-action 위임 처리
    const target = e.target.closest('[data-action]');
    if (!target) return;

    const action = target.dataset.action;

    if (action === 'toggle-user-menu') {
        toggleUserMenu();
    } else if (action === 'bookmark') {
        toggleBookmark(target);
    } else if (action === 'like') {
        toggleLike(target);
    } else if (action === 'delete-account') {
        handleDeleteAccount();
    } else if (action === 'google-pw-info') {
        showAlert(target.dataset.message || 'Google 계정은 Google에서 비밀번호를 변경해주세요.');
    }
});

/* ===== 회원 탈퇴 처리 ===== */
async function handleDeleteAccount() {
    const ok = await showConfirm('정말 탈퇴하시겠습니까?\n모든 데이터가 삭제되며 복구할 수 없습니다.');
    if (ok) {
        document.getElementById('deleteForm').submit();
    }
}

/* ===== CSRF 토큰 읽기 (XSRF-TOKEN 쿠키 → X-XSRF-TOKEN 헤더) ===== */
function getCsrfHeaders() {
    const token = document.cookie.split('; ')
        .find(r => r.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];
    return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
}

/* ===== 좋아요 토글 ===== */
async function toggleLike(btn) {
    const articleUrl = btn.getAttribute('data-url');
    if (!articleUrl) return;

    try {
        const res = await fetch('/like/toggle', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
            body: JSON.stringify({ articleUrl })
        });

        if (res.status === 401) {
            await showAlert('좋아요하려면 로그인이 필요합니다.');
            window.location.href = '/auth/login';
            return;
        }

        if (!res.ok) return;

        const data = await res.json();
        const icon = btn.querySelector('span:first-child');
        const count = btn.querySelector('.like-count');

        if (data.liked) {
            btn.classList.add('active');
            icon.textContent = '❤️';
            if (count) count.textContent = parseInt(count.textContent) + 1;
        } else {
            btn.classList.remove('active');
            icon.textContent = '🤍';
            if (count) count.textContent = Math.max(0, parseInt(count.textContent) - 1);
        }

    } catch (e) {
        console.error('좋아요 오류:', e);
    }
}

/* ===== 북마크 토글 ===== */
async function toggleBookmark(btn) {
    const articleUrl = btn.getAttribute('data-url');
    if (!articleUrl) return;

    try {
        const res = await fetch('/bookmark/toggle', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
            body: JSON.stringify({ articleUrl })
        });

        if (res.status === 401) {
            await showAlert('북마크하려면 로그인이 필요합니다.');
            window.location.href = '/auth/login';
            return;
        }

        if (!res.ok) {
            console.error('북마크 요청 실패:', res.status);
            return;
        }

        const data = await res.json();
        const span = btn.querySelector('span') || btn;

        if (data.bookmarked) {
            btn.classList.add('active');
            span.textContent = '★';
        } else {
            btn.classList.remove('active');
            span.textContent = '☆';
        }

    } catch (e) {
        console.error('북마크 오류:', e);
    }
}
