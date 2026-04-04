document.addEventListener('DOMContentLoaded', function () {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('sidebar-toggle');
    if (!sidebar || !toggleBtn) return;
    const key = 'sidebarCollapsed';
    if (localStorage.getItem(key) === 'true') {
        sidebar.classList.add('collapsed');
    }
    toggleBtn.addEventListener('click', function () {
        sidebar.classList.toggle('collapsed');
        localStorage.setItem(key, sidebar.classList.contains('collapsed'));
    });
});
