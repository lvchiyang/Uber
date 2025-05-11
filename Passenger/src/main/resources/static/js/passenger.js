// 全局变量
let passengerInfo = {
    name: null,
    balance: 0,
    completedOrders: 0,
    totalSpending: 0
};

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


let driverLocation = null;   // driverLocation = { x, y }
let selectingMode = null; // 'start', 'end' 或 null
let baseUrl = '';
let eventSource = null;

// DOM元素
const currentTimeEl = document.getElementById('current-time');
const balanceDisplayEl = document.getElementById('balance-display');
const completedOrdersEl = document.getElementById('completed-orders');
const totalSpendingEl = document.getElementById('total-spending');
const passengerStatusEl = document.getElementById('passenger-status');
const passengerNameEl = document.getElementById('passenger-name');
const loginPanelEl = document.getElementById('login-panel');
const rechargePanelEl = document.getElementById('recharge-panel');
const orderPanelEl = document.getElementById('order-panel');
const loginBtnEl = document.getElementById('login-btn');
const nameInputEl = document.getElementById('name-input');
const rechargeBtnEl = document.getElementById('recharge-btn');
const amountInputEl = document.getElementById('amount-input');
const selectStartBtnEl = document.getElementById('select-start-btn');
const selectEndBtnEl = document.getElementById('select-end-btn');
const createOrderBtnEl = document.getElementById('create-order-btn');
const startLocationEl = document.getElementById('start-location');
const endLocationEl = document.getElementById('end-location');
const orderIdEl = document.getElementById('order-id');
const orderStartEl = document.getElementById('order-start');
const orderEndEl = document.getElementById('order-end');
const orderPriceEl = document.getElementById('order-price');
const driverLocationEl = document.getElementById('driver-location');
const orderStatusEl = document.getElementById('order-status');
const statusMessagesEl = document.getElementById('status-messages');
const checkConnectionBtnEl = document.getElementById('check-connection-btn');
const connectionStatusEl = document.getElementById('connection-status');

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', function() {
    // 添加初始状态信息
    showStatusMessage('欢迎使用优步乘客端', 'info');
    
    // 设置基础URL
    baseUrl = 'http://localhost:8081';
    
    
    // 初始化地图
    initMap();
    
    // 更新当前时间
    updateCurrentTime();
    setInterval(updateCurrentTime, 1000);
    
    // 绑定事件监听器
    loginBtnEl.addEventListener('click', handleLogin);
    rechargeBtnEl.addEventListener('click', handleRecharge);
    selectStartBtnEl.addEventListener('click', () => startSelectingTile('start'));
    selectEndBtnEl.addEventListener('click', () => startSelectingTile('end'));
    createOrderBtnEl.addEventListener('click', handleCreateOrder);
    checkConnectionBtnEl.addEventListener('click', checkConnection);

    // 检查到窗口大小变化，更新地图大小
    window.addEventListener('resize', function() {
        initMap();
    });
    
    // 检查连接状态
    checkConnection();

    // 连接WebSocket
    connect();
    
    // 在页面关闭时断开连接
    window.addEventListener('beforeunload', function() {
        disconnect();
    });

});


var stompClient = null;

// 连接到 WebSocket 服务器
function connect() {
    console.log('正在连接WebSocket服务器...');
    try {
        // 使用完整的URL
        var socket = new SockJS(`${baseUrl}/PassengerWebSocket`);
        stompClient = Stomp.over(socket);
        
        // 添加连接选项
        var connectHeaders = {
            'heart-beat': '10000,10000'
        };
        
        // 启用调试日志
        stompClient.debug = function(str) {
            console.log('STOMP: ' + str);
        };
        
        stompClient.connect(connectHeaders, 
            // 连接成功回调
            function(frame) {
                console.log('WebSocket连接成功:', frame);
                showStatusMessage('WebSocket连接已建立', 'success');
                
                // 订阅消息通道
                try {
                    stompClient.subscribe('/topic/passenger', function(data) {
                        console.log('收到乘客信息更新:', data);
                        try {
                            passengerInfo = JSON.parse(data.body);
                            updatePassengerUI();
                        } catch (e) {
                            console.error('解析乘客信息失败:', e);
                        }
                    });

                    stompClient.subscribe('/topic/order', function(data) {
                        console.log('收到订单状态更新:', data);
                        try {
                            order = JSON.parse(data.body);
                            updateOrderPanel(order);
                        } catch (e) {
                            console.error('解析订单信息失败:', e);
                        }
                    });

                    stompClient.subscribe('/topic/location', function(data) {
                        console.log('收到司机位置更新:', data);
                        try {
                            driverLocation = JSON.parse(data.body);
                            updateDriverLocation(driverLocation);
                        } catch (e) {
                            console.error('解析司机位置信息失败:', e);
                        }
                    });
                    
                    console.log('所有消息通道订阅成功');
                } catch (e) {
                    console.error('订阅消息通道失败:', e);
                    showStatusMessage('订阅消息通道失败', 'error');
                }
            },
            // 连接错误回调
            function(error) {
                console.error('WebSocket连接错误:', error);
                showStatusMessage('WebSocket连接失败，5秒后重试...', 'error');
                setTimeout(connect, 5000);
            }
        );
    } catch (e) {
        console.error('创建WebSocket连接失败:', e);
        showStatusMessage('创建WebSocket连接失败，5秒后重试...', 'error');
        setTimeout(connect, 5000);
    }
}

// 断开WebSocket连接
function disconnect() {
    if (stompClient !== null) {
        try {
            stompClient.disconnect();
            console.log('WebSocket连接已断开');
            showStatusMessage('WebSocket连接已断开', 'info');
        } catch (e) {
            console.error('断开WebSocket连接失败:', e);
        }
    }
}

// 更新当前时间
function updateCurrentTime() {
    const now = new Date();
    currentTimeEl.textContent = now.toLocaleString('zh-CN');
}

// 检查连接状态
function checkConnection() {
    fetch(`${baseUrl}/passenger/test`)
        .then(response => {
            if (response.ok) {
                connectionStatusEl.textContent = '服务正常';
                connectionStatusEl.className = 'connected';
                showStatusMessage('服务连接正常', 'success');
            } else {
                throw new Error('服务不可用');
            }
        })
        .catch(error => {
            connectionStatusEl.textContent = '连接失败';
            connectionStatusEl.className = 'disconnected';
            showStatusMessage('服务连接失败', 'error');
            console.error('连接检查错误:', error);
        });
}

// 初始化地图
function initMap() {
    const canvas = document.getElementById('map-canvas');
    
    const ctx = canvas.getContext('2d');
    const mapContainer = document.querySelector('.map');
    
    // 设置canvas尺寸适应容器
    canvas.width = mapContainer.clientWidth;  // 适应地图容器宽度
    canvas.height = mapContainer.clientWidth; // 保持正方形
    
    // 添加点击事件
    canvas.addEventListener('click', handleCanvasClick);
    
    // 绘制空白地图网格
    drawMap(ctx, canvas.width, canvas.height);
    
    // 重新绘制已选择的点和司机位置
    updateMapDisplay();
}

// 处理Canvas点击事件
function handleCanvasClick(event) {
    if (!selectingMode) return;
    
    const canvas = document.getElementById('map-canvas');
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    
    // 计算点击的格子坐标
    const cellSize = canvas.width / 10;
    const gridX = Math.floor(x / cellSize);
    const gridY = Math.floor(y / cellSize);
    
    // 确保点击在有效区域内 (0-9)
    if (gridX >= 0 && gridX < 10 && gridY >= 0 && gridY < 10) {
        handleTileClick(gridX, gridY);
    }
}

// 处理格子点击
function handleTileClick(x, y) {
    if (selectingMode === 'start') {
        order.startLocation = { x: x, y: y };
        startLocationEl.textContent = `(${x}, ${y})`;
        selectingMode = null;
        showStatusMessage('已选择起点位置', 'success');
    } else if (selectingMode === 'end') {
        order.endLocation = { x, y };  
        endLocationEl.textContent = `(${x}, ${y})`;
        selectingMode = null;
        showStatusMessage('已选择终点位置', 'success');
    }
    
    updateMapDisplay();
    checkCreateOrderButton();
}

// 检查是否可以创建订单
function checkCreateOrderButton() {
    if (order.startLocation && order.endLocation && passengerInfo.name) {
        createOrderBtnEl.disabled = false;
    } else {
        createOrderBtnEl.disabled = true;
    }
}

// 更新地图显示
function updateMapDisplay() {
    const canvas = document.getElementById('map-canvas');
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    
    // 清空画布
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // 绘制地图
    drawMap(ctx, canvas.width, canvas.height);
    
    // 绘制选中的起点和终点
    if (order.startLocation) {
        drawTileHighlight(ctx, order.startLocation.x, order.startLocation.y, '#E8C4C4', '起点');
    }
    
    if (order.endLocation) {
        drawTileHighlight(ctx, order.endLocation.x, order.endLocation.y, '#B0C5A4', '终点');
    }
    
    // 绘制司机位置（如果有）
    if (driverLocation) {
        drawEntity(ctx, driverLocation.x, driverLocation.y, '#B4CDE6', '司机');
    }
}

// 绘制地图
function drawMap(ctx, width, height) {
    // 绘制背景为白色
    ctx.fillStyle = '#FBF9F3';
    ctx.fillRect(0, 0, width, height);
    
    // 计算单元格大小
    const cellSize = width / 10;
    
    // 绘制网格线
    ctx.strokeStyle = '#E6DFD5';
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
}

// 绘制高亮格子（起点/终点）
function drawTileHighlight(ctx, x, y, color, label) {
    const canvas = document.getElementById('map-canvas');
    const cellSize = canvas.width / 10;
    
    // 计算位置
    const tileX = x * cellSize;
    const tileY = y * cellSize;
    
    // 绘制半透明高亮
    ctx.fillStyle = color + '80'; // 添加50%透明度
    ctx.fillRect(tileX, tileY, cellSize, cellSize);
    
    // 绘制标签
    ctx.font = 'bold 14px Arial';
    ctx.fillStyle = '#5F734C';  // 更改为莫兰迪绿色
    ctx.textAlign = 'center';
    ctx.fillText(label, tileX + cellSize / 2, tileY + cellSize / 2);
}

// 绘制实体（司机）
function drawEntity(ctx, x, y, color, label) {
    const canvas = document.getElementById('map-canvas');
    const cellSize = canvas.width / 10;
    const radius = cellSize * 0.3;
    
    // 计算实体在画布上的位置（中心点）
    const centerX = x * cellSize + cellSize / 2;
    const centerY = y * cellSize + cellSize / 2;
    
    // 绘制实体
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
    ctx.fillStyle = color;
    ctx.fill();
    ctx.strokeStyle = '#576F72';
    ctx.lineWidth = 2;
    ctx.stroke();
    
    // 绘制标签
    ctx.font = 'bold 12px Arial';
    ctx.fillStyle = '#5F734C';  // 更改为莫兰迪绿色
    ctx.textAlign = 'center';
    ctx.fillText(label, centerX, centerY + radius + 15);
}

// 开始选择格子模式
function startSelectingTile(mode) {
    selectingMode = mode;
    if (mode === 'start') {
        showStatusMessage('请在地图上选择起点位置');
    } else if (mode === 'end') {
        showStatusMessage('请在地图上选择终点位置');
    }
}


// 显示状态消息
function showStatusMessage(message, type = 'info') {
    const messageEl = document.createElement('div');
    messageEl.className = `status-message ${type}`;
    messageEl.textContent = message;
    
    statusMessagesEl.appendChild(messageEl);
    statusMessagesEl.scrollTop = statusMessagesEl.scrollHeight;
    
    // 5秒后自动删除
    setTimeout(() => {
        messageEl.remove();
    }, 5000);
}

// 处理登录
function handleLogin() {
    const name = nameInputEl.value.trim();
    
    if (!name) {
        showStatusMessage('请输入姓名', 'error');
        return;
    }
    
    fetch(`${baseUrl}/passenger/${name}/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: name })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('登录失败');
        }
        return response.json();
    })
    .then(data => {
        passengerInfo = data;
        updatePassengerUI();
        
        // 隐藏登录面板，显示充值面板
        loginPanelEl.classList.add('hidden');
        rechargePanelEl.classList.remove('hidden');
        
        showStatusMessage(`欢迎，${passengerInfo.name}！`, 'success');
        passengerStatusEl.textContent = '在线';
    })
    .catch(error => {
        console.error('登录错误:', error);
        showStatusMessage('登录失败，请重试', 'error');
    });
}

// 处理充值
function handleRecharge() {
    const amount = parseFloat(amountInputEl.value);
    
    if (isNaN(amount) || amount <= 0) {
        showStatusMessage('请输入有效的充值金额', 'error');
        return;
    }
    
    fetch(`${baseUrl}/passenger/${passengerInfo.name}/recharge`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ 
            amount
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('充值失败');
        }
        return response.json();
    })
    .then(data => {
        passengerInfo = data;
        updatePassengerUI();
        
        amountInputEl.value = '';
        showStatusMessage(`成功充值 ${amount} 元`, 'success');
    })
    .catch(error => {
        console.error('充值错误:', error);
        showStatusMessage('充值失败，请重试', 'error');
    });
}

// 更新乘客UI
function updatePassengerUI() {
    passengerNameEl.textContent = passengerInfo.name || '未登录';
    balanceDisplayEl.textContent = passengerInfo.balance ? passengerInfo.balance.toFixed(2) : '0.00';
    completedOrdersEl.textContent = passengerInfo.completedOrders || '0';
    totalSpendingEl.textContent = passengerInfo.totalSpending ? passengerInfo.totalSpending.toFixed(2) : '0.00';
    
    // 更新乘客状态
    if (order.orderId) {
        passengerStatusEl.textContent = '已有订单';
    } else {
        passengerStatusEl.textContent = '在线';
    }
    
    checkCreateOrderButton();
}

// 创建订单
function handleCreateOrder() {
    if (!order.startLocation || !order.endLocation) {
        showStatusMessage('请先选择起点和终点', 'error');
        return;
    }
    
    const orderData = {
        startLocation: order.startLocation,
        endLocation: order.endLocation
    };
    
    fetch(`${baseUrl}/passenger/${passengerInfo.name}/creater`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(orderData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('创建订单失败');
        }
        return response.json();
    })
    .then(data => {
        order = data;

        if(order.status == "乘客未登录"){
            showStatusMessage('请先登录', 'error');
            return;
        }
        if(order.status == "乘客余额不足"){
            showStatusMessage('乘客余额不足，请充值', 'error');
            return;
        }
        
        console.log('收到订单数据:', order); // 添加调试日志
        
        // 显示订单面板
        orderPanelEl.classList.remove('hidden');
        
        // 更新订单信息
        updateOrderPanel(order);
        
        // 更新乘客状态
        passengerStatusEl.textContent = order.status;
        
        showStatusMessage('订单已创建，等待司机接单', 'success');
    })
    .catch(error => {
        console.error('创建订单错误:', error);
        
        // 添加更多详细日志
        if (error.response) {
            console.error('错误响应:', error.response);
        }
        
        // 显示错误消息
        showStatusMessage('创建订单失败，请重试', 'error');
    });
}

// 更新订单面板
function updateOrderPanel(order) {
    if (!order) return;
    
    console.log('更新订单面板:', order); // 调试日志

    // 根据订单状态更新UI
    switch(order.status) {
        case '等待安排司机':
            document.getElementById('order-panel').classList.remove('hidden');
            break;
        case '司机接单途中':
            document.getElementById('order-panel').classList.remove('hidden');
            passengerStatusEl.textContent = '司机接单途中';
            break;
        case '司机送客途中':
            document.getElementById('order-panel').classList.remove('hidden');
            passengerStatusEl.textContent = '司机送客途中';
            break;
        case '订单完成':
            document.getElementById('order-panel').classList.add('hidden');
            passengerStatusEl.textContent = '订单完成';
            handleOrderComplete(order);
            break;
    }
    
    // 添加状态消息
    showStatusMessage(`订单状态更新: ${convertOrderStatus(order.status)}`);
    
    orderIdEl.textContent = order.orderId || '无';
    
    // 正确显示起点和终点位置
    if (order.startLocation) {
        orderStartEl.textContent = `(${order.startLocation.x}, ${order.startLocation.y})`;
    } else {
        orderStartEl.textContent = '未设置';
    }
    
    if (order.endLocation) {
        orderEndEl.textContent = `(${order.endLocation.x}, ${order.endLocation.y})`;
    } else {
        orderEndEl.textContent = '未设置';
    }
    
    orderPriceEl.textContent = order.price ? `${order.price.toFixed(2)} 元` : '计算中...';
    orderStatusEl.textContent = order.status;
}

// 添加缺失的函数
function convertOrderStatus(status) {
    switch(status) {
        case '等待安排司机': return '等待司机接单';
        case '司机接单途中': return '司机正在赶来接您';
        case '司机送客途中': return '司机已接到您，正在前往目的地';
        case '订单完成': return '订单已完成';
        case '已取消': return '订单已取消';
        case '订单创建失败': return '订单创建失败';
        default: return status;
    }
}

// 更新司机位置
function updateDriverLocation(locationData) {
    if (!locationData) return;
    
    // 确保位置数据有x,y坐标
    if (locationData.x !== undefined && locationData.y !== undefined) {
        // 更新司机位置标记，这个标记会在地图上显示
        driverLocation = {
            x: locationData.x,
            y: locationData.y
        };
        
        // 更新地图显示，重新绘制地图上的司机位置
        updateMapDisplay();
    } else {
        console.error('无效的司机位置数据', locationData);
    }
}


// 处理订单完成
function handleOrderComplete(billInfo) {
    // 更新账户信息
    if (billInfo.balance !== undefined) {
        passengerInfo.balance = billInfo.balance;
    }
    
    // 记录完成状态
    showStatusMessage('订单已完成', 'success');
    
    // 重置订单状态
    order = {
        orderId: null,
        passengerName: null,
        driverName: null,
        startLocation: null,
        endLocation: null,
        price: null,
        status: null,
        createTime: null,
        completeTime: null
    };
    driverLocation = null;
    
    // 更新UI
    updatePassengerUI();
    
    // 隐藏订单面板
    orderPanelEl.classList.add('hidden');
    
    // 更新地图显示 - 清除所有标记
    updateMapDisplay();
    
    console.log('订单完成，已清除所有订单数据');
}