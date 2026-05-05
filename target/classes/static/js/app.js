// Tab switching
function switchTab(tabId) {
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    const content = document.getElementById('tab-' + tabId);
    if (content) content.classList.add('active');
    const btn = document.querySelector('[data-tab="' + tabId + '"]');
    if (btn) btn.classList.add('active');
}

// Thymeleaf 3.1+ güvenlik kısıtı nedeniyle th:onclick yerine data-* kullanıyoruz.
// Sil butonları: <button th:data-formid="..." th:data-name="..." onclick="confirmDeleteBtn(this)">
function confirmDeleteBtn(btn) {
    var name = btn.getAttribute('data-name') || 'Bu kayıt';
    var formId = btn.getAttribute('data-formid');
    if (confirm(name + ' silinecek. Emin misiniz?')) {
        document.getElementById(formId).submit();
    }
}

// Day picker: max 3 seçim
document.addEventListener('change', function(e) {
    if (e.target && e.target.classList.contains('day-checkbox')) {
        var picker = e.target.closest('.day-picker');
        if (!picker) return;
        var checked = picker.querySelectorAll('input:checked');
        if (checked.length > 3) {
            e.target.checked = false;
        }
        var hint = picker.querySelector('.day-hint');
        if (hint) {
            var c = picker.querySelectorAll('input:checked').length;
            hint.textContent = c + '/3 gün seçili' + (c === 0 ? ' (boş = otomatik)' : '');
        }
    }
});

// Alert otomatik kapat
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.alert').forEach(function(alert) {
        setTimeout(function() {
            alert.style.transition = 'opacity 0.4s';
            alert.style.opacity = '0';
            setTimeout(function() { alert.remove(); }, 400);
        }, 4000);
    });
});