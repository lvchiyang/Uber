// 全局变量
let mapGrid = [];
let drivers = [];
let passengers = [];
let activeOrders = [];
let allOrders = [];
let allDrivers = [];
let allPassengers = [];
let currentPage = 1;
let ordersPerPage = 10;
let activeTab = 'map-view';


let passenger = {
    name: null,
    balance: 0,
    completedOrders: 0,
    totalSpending: 0
}


let driver = {
    name: null,
    status: null,
    currentLocation: null,
    currentOrderId: null,
    totalEarnings: 0.00,
    completedOrders: 0
}

let order = {
    orderId: null,
    passengerName: null,
    driverName: null,
    startLocation: null,
    endLocation: null,
    price: null,
    status: null, // 等待安排司机, 司机接单途中, 司机送客途中, 订单完成
    createTime: null,
    completeTime: null
}

// 页面加载完成后初始化应用
document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    initializeMap();
    updateCurrentTime();
    
    // 定时刷新数据
    fetchAllData();
    setInterval(fetchAllData, 5000);
    setInterval(updateCurrentTime, 1000);
    
    // 初始化搜索和筛选功能
    initializeSearch();

    // 立即加载订单历史数据
    fetch(baseUrl+'/platform/orders')
        .then(response => response.json())
        .then(data => {
            allOrders = data;
            console.log('订单数据加载成功:', allOrders.length);
            // 如果当前是订单历史标签页，立即渲染
            if (activeTab === 'orders-view') {
                renderOrderHistory();
            }
        })
        .catch(error => console.error('获取订单历史失败:', error));
});


baseUrl = 'http://localhost:8083';


// 初始化标签页切换
function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    
    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabId = button.getAttribute('data-tab');
            
            // 更新激活的标签按钮
            tabButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');
            
            // 更新显示的内容
            tabContents.forEach(content => content.classList.remove('active'));
            document.getElementById(tabId).classList.add('active');
            
            activeTab = tabId;
            
            // 如果切换到账户管理或订单历史，刷新数据
            if (tabId === 'accounts-view') {
                renderPassengerAccounts();
                renderDriverAccounts();
            } else if (tabId === 'orders-view') {
                renderOrderHistory();
            }
        });
    });
}

// 初始化地图网格
function initializeMap() {
    const canvas = document.getElementById('map-canvas');
    const ctx = canvas.getContext('2d');
    const mapContainer = document.getElementById('map-grid');
    
    // 绘制空白地图
    drawEmptyMap(ctx, canvas.width, canvas.height);
    
    // 监听窗口大小变化，重新调整地图
    window.addEventListener('resize', function() {
        // 防止重绘时超出边界
        drawEmptyMap(ctx, canvas.width, canvas.height);
        updateMapView();
    });
}

// 绘制空白地图
function drawEmptyMap(ctx, width, height) {
    // 清空画布
    ctx.clearRect(0, 0, width, height);
    
    // 绘制背景为白色
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, width, height);
    
    // 计算单元格大小 - 确保完全填充画布
    const cellSize = Math.min(width, height) / 10;
    
    // 绘制网格线
    ctx.strokeStyle = '#e0e0e0';
    ctx.lineWidth = 1;
    
    // 绘制水平线
    for (let i = 0; i <= 10; i++) {
        ctx.beginPath();
        ctx.moveTo(0, i * cellSize);
        ctx.lineTo(width, i * cellSize);
        ctx.stroke();
    }
    
    // 绘制垂直线
    for (let i = 0; i <= 10; i++) {
        ctx.beginPath();
        ctx.moveTo(i * cellSize, 0);
        ctx.lineTo(i * cellSize, height);
        ctx.stroke();
    }
    
    // 存储地图单元格尺寸
    window.mapCellSize = cellSize;
}

// 更新当前时间
function updateCurrentTime() {
    const timeElement = document.getElementById('current-time');
    const now = new Date();
    const options = { 
        year: 'numeric', 
        month: 'numeric', 
        day: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit', 
        second: '2-digit' 
    };
    timeElement.innerText = now.toLocaleString('zh-CN', options);
}

// 获取所有数据
function fetchAllData() {
    // 获取司机位置数据
    fetch(baseUrl+'/platform/drivers/online')
        .then(response => response.json())
        .then(data => {
            drivers = data;
            updateMapView();
            updateDriverList();
        })
        .catch(error => console.error('获取司机位置失败:', error));
    
    // 获取活动订单数据
    fetch(baseUrl+'/platform/orders/active')
        .then(response => response.json())
        .then(data => {
            activeOrders = data;
            updateActiveOrderList();
        })
        .catch(error => console.error('获取活动订单失败:', error));
    
    // 如果当前是账户管理标签页，获取所有账户数据
    if (activeTab === 'accounts-view') {
        fetch(baseUrl+'/platform/passengers')
            .then(response => response.json())
            .then(data => {
                allPassengers = data;
                renderPassengerAccounts();
            })
            .catch(error => console.error('获取乘客账户失败:', error));
        
        fetch(baseUrl+'/platform/drivers')
            .then(response => response.json())
            .then(data => {
                allDrivers = data;
                renderDriverAccounts();
            })
            .catch(error => console.error('获取司机账户失败:', error));
    }
    
    // 如果当前是订单历史标签页，获取订单历史数据
    if (activeTab === 'orders-view') {
        fetch(baseUrl+'/platform/orders')
            .then(response => response.json())
            .then(data => {
                allOrders = data;
                renderOrderHistory();
            })
            .catch(error => console.error('获取订单历史失败:', error));
    }
}

// 更新地图视图
function updateMapView() {
    const canvas = document.getElementById('map-canvas');
    const ctx = canvas.getContext('2d');
    
    // 重绘地图
    drawEmptyMap(ctx, canvas.width, canvas.height);
    
    // 获取单元格大小
    const cellSize = window.mapCellSize;
    
    // 绘制所有在线司机位置
    drivers.forEach(driver => {
        if (driver.currentLocation) {
            // 计算司机在画布上的位置
            const x = driver.currentLocation.x * cellSize + cellSize / 2;
            const y = driver.currentLocation.y * cellSize + cellSize / 2;
            
            // 绘制司机标记
            ctx.beginPath();
            ctx.arc(x, y, cellSize * 0.3, 0, Math.PI * 2);
            ctx.fillStyle = driver.status === '闲逛' ? '#34a853' : '#ea4335';
            ctx.fill();
            ctx.strokeStyle = 'white';
            ctx.lineWidth = 2;
            ctx.stroke();
            
            // 添加司机名称标签
            ctx.fillStyle = '#333';
            ctx.font = '12px Arial';
            ctx.textAlign = 'center';
            ctx.fillText(driver.name, x, y - cellSize * 0.4);
        }
    });
    
    // 更新在线司机数量
    document.getElementById('driver-count').innerText = drivers.length;

}


// 更新司机列表
function updateDriverList() {
    const driverListElement = document.getElementById('driver-list');
    driverListElement.innerHTML = '';
    
    document.getElementById('driver-count').innerText = drivers.length;
    
    if (drivers.length === 0) {
        driverListElement.innerHTML = '<tr><td colspan="3" class="no-data">暂无在线司机</td></tr>';
        return;
    }
    
    drivers.forEach(driver => {
        const row = document.createElement('tr');
        
        const status = driver.status === '闲逛' ? '闲逛' : '接单';
        
        row.innerHTML = `
            <td>${driver.name || '未知'}</td>
            <td>(${driver.currentLocation ? driver.currentLocation.x : '?'}, ${driver.currentLocation ? driver.currentLocation.y : '?'})</td>
            <td><span class="driver-status">${status}</span></td>
        `;
        
        driverListElement.appendChild(row);
    });
}

// 更新活动订单列表
function updateActiveOrderList() {
    const orderListElement = document.getElementById('active-order-list');
    orderListElement.innerHTML = '';
    
    document.getElementById('order-count').innerText = activeOrders.length;
    
    if (activeOrders.length === 0) {
        orderListElement.innerHTML = '<tr><td colspan="5" class="no-data">暂无活动订单</td></tr>';
        return;
    }
    
    activeOrders.forEach(order => {
        const row = document.createElement('tr');
        row.setAttribute('data-order-id', order.orderId);
        row.classList.add('order-row');
        
        let statusText = order.status;
        
        row.innerHTML = `
            <td>${order.orderId || '未知'}</td>
            <td>${order.passengerName || '未知'}</td>
            <td>${order.driverName || '暂无'}</td>
            <td>${statusText}</td>
            <td><button class="cancel-order-btn">取消订单</button></td>
        `;
        
        orderListElement.appendChild(row);
    });
    
    // 添加取消订单按钮的点击事件
    document.querySelectorAll('.cancel-order-btn').forEach(button => {
        button.addEventListener('click', function(e) {
            e.stopPropagation();
            const row = this.closest('.order-row');
            const orderId = row.getAttribute('data-order-id');
            if (confirm('确定要取消订单 ' + orderId + ' 吗？')) {
                cancelOrder(orderId);
            }
        });
    });
}

// 取消订单
function cancelOrder(orderId) {
    fetch(`${baseUrl}/platform/orders/${orderId}/cancel`, {
        method: 'DELETE'
    })
    .then(response => response.json())
    .then(result => {
        if (result === true) {
            alert('订单取消成功');
            // 刷新数据
            fetchAllData();
        } else {
            alert('订单取消失败，可能订单已完成或不存在');
        }
    })
    .catch(error => {
        console.error('取消订单失败:', error);
        alert('取消订单失败，请稍后重试');
    });
}

// 渲染乘客账户
function renderPassengerAccounts() {
    const tableBody = document.querySelector('#passenger-table tbody');
    tableBody.innerHTML = '';
    
    if (allPassengers.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="5" style="text-align: center;">暂无乘客数据</td>';
        tableBody.appendChild(row);
        return;
    }
    
    allPassengers.forEach(passenger => {
        const row = document.createElement('tr');
        
        
        row.innerHTML = `
            <td>${passenger.name}</td>
            <td>¥${passenger.balance ? passenger.balance.toFixed(2) : '0.00'}</td>
        `;
        
        tableBody.appendChild(row);
    });
}


// 渲染司机账户
function renderDriverAccounts() {
    const tableBody = document.querySelector('#driver-table tbody');
    tableBody.innerHTML = '';
    
    if (allDrivers.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="6" style="text-align: center;">暂无司机数据</td>';
        tableBody.appendChild(row);
        return;
    }
    
    allDrivers.forEach(driver => {
        const row = document.createElement('tr');
        
        row.innerHTML = `
            <td>${driver.name}</td>
            <td>${driver.status}</td>
            <td>¥${driver.totalEarnings ? driver.totalEarnings.toFixed(2) : '0.00'}</td>
            <td>${driver.completedOrders || 0}</td>
            <td>${driver.currentLocation ? `(${driver.currentLocation.x}, ${driver.currentLocation.y})` : '未知'}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

// 初始化搜索功能
function initializeSearch() {
    const orderSearchInput = document.getElementById('order-search');
    if (orderSearchInput) {
        orderSearchInput.addEventListener('input', function() {
            currentPage = 1; // 重置到第一页
            renderOrderHistory();
        });
    }
}

// 修改renderOrderHistory函数中的筛选逻辑
function renderOrderHistory() {
    const tableBody = document.getElementById('order-history');
    if (!tableBody) {
        console.error('找不到order-history元素');
        return;
    }
    
    tableBody.innerHTML = '';
    console.log('正在渲染订单历史，数据数量:', allOrders ? allOrders.length : 0);
    
    // 如果订单数据为空
    if (!allOrders || allOrders.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="9" style="text-align: center;">暂无订单数据</td>';
        tableBody.appendChild(row);
        
        // 更新分页信息
        document.getElementById('page-info').innerText = '第 0 页，共 0 页';
        document.getElementById('prev-page').disabled = true;
        document.getElementById('next-page').disabled = true;
        return;
    }
    
    // 获取搜索关键词
    const searchKeyword = document.getElementById('order-search') ? 
        document.getElementById('order-search').value.toLowerCase() : '';
    
    // 筛选订单
    let filteredOrders = allOrders;
    
    // 应用搜索过滤
    if (searchKeyword) {
        filteredOrders = filteredOrders.filter(order => 
            (order.orderId && order.orderId.toLowerCase().includes(searchKeyword)) ||
            (order.passengerName && order.passengerName.toLowerCase().includes(searchKeyword)) ||
            (order.driverName && order.driverName.toLowerCase().includes(searchKeyword)) ||
            (order.status && order.status.toLowerCase().includes(searchKeyword))
        );
    }
    
    // 分页
    const totalPages = Math.ceil(filteredOrders.length / ordersPerPage);
    if (currentPage > totalPages && totalPages > 0) {
        currentPage = totalPages;
    }
    
    document.getElementById('page-info').innerText = `第 ${currentPage} 页，共 ${totalPages} 页`;
    document.getElementById('prev-page').disabled = currentPage <= 1;
    document.getElementById('next-page').disabled = currentPage >= totalPages;
    
    // 获取当前页订单
    const startIndex = (currentPage - 1) * ordersPerPage;
    const endIndex = startIndex + ordersPerPage;
    const currentPageOrders = filteredOrders.slice(startIndex, endIndex);
    
    if (currentPageOrders.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="9" style="text-align: center;">无符合条件的订单</td>';
        tableBody.appendChild(row);
        return;
    }
    
    // 渲染订单
    currentPageOrders.forEach(order => {
        const row = document.createElement('tr');
        
        let statusText = order.status || '未知';
        
        row.innerHTML = `
            <td>${order.orderId || '未知'}</td>
            <td>${order.passengerName || '未知'}</td>
            <td>${order.driverName || '暂无'}</td>
            <td>(${order.startLocation ? order.startLocation.x : '?'}, ${order.startLocation ? order.startLocation.y : '?'})</td>
            <td>(${order.endLocation ? order.endLocation.x : '?'}, ${order.endLocation ? order.endLocation.y : '?'})</td>
            <td>¥${order.price ? order.price.toFixed(2) : '未计算'}</td>
            <td>${statusText}</td>
            <td>${formatDate(order.createTime)}</td>
            <td>${order.completeTime ? formatDate(order.completeTime) : '-'}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

// 添加日期格式化函数
function formatDate(dateString) {
    if (!dateString) return '未知';
    try {
        const date = new Date(dateString);
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
            return '日期无效';
        }
        return date.toLocaleString('zh-CN', { 
            year: 'numeric', 
            month: 'numeric', 
            day: 'numeric', 
            hour: '2-digit', 
            minute: '2-digit', 
            second: '2-digit' 
        });
    } catch (e) {
        console.error('格式化日期出错:', e, dateString);
        return '日期错误';
    }
}

// 删除订单记录
function deleteOrder() {
    const orderId = document.getElementById('order-delete').value.trim();
    if (!orderId) {
        alert('请输入要删除的订单ID');
        return;
    }

    if (!confirm('确定要删除订单 ' + orderId + ' 吗？此操作不可恢复。')) {
        return;
    }

    fetch(baseUrl + '/platform/orders/' + orderId, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            alert('订单已成功删除');
            document.getElementById('order-delete').value = '';
            // 刷新订单列表
            fetchAllData();
        } else if (response.status === 404) {
            alert('未找到指定的订单');
        } else {
            alert('删除订单失败');
        }
    })
    .catch(error => {
        console.error('删除订单时发生错误:', error);
        alert('删除订单时发生错误');
    });
}

// 分页功能
function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        renderOrderHistory();
    }
}

function nextPage() {
    const totalPages = Math.ceil(allOrders.length / ordersPerPage);
    if (currentPage < totalPages) {
        currentPage++;
        renderOrderHistory();
    }
}