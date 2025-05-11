// 全局变量
let pollingInterval = null; // 用于轮询状态的定时器
let mapContext = null;
let gridSize = 10;
let cellSize = 60; // 增大单元格尺寸以填满更大的画布

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
    price: 0,
    status: null, // 等待安排司机, 司机接单途中, 司机送客途中, 订单完成
    createTime: null,
    completeTime: null
}


// DOM 元素
const driverNameElement = document.getElementById('driver-name');
const driverStatusElement = document.getElementById('driver-status');
const currentLocationElement = document.getElementById('current-location');
const totalEarningsElement = document.getElementById('total-earnings');
const completedOrdersElement = document.getElementById('completed-orders');

const mapCanvas = document.getElementById('map-canvas');
const checkConnectionBtn = document.getElementById('check-connection-btn');
const connectionStatus = document.getElementById('connection-status');
const loginPanel = document.getElementById('login-panel');
const orderPanel = document.getElementById('order-panel');
const nameInput = document.getElementById('name-input');
const loginBtn = document.getElementById('login-btn');
const offlineBtn = document.getElementById('offline-btn');

const currentTimeElement = document.getElementById('current-time');
const statusMessages = document.getElementById('status-messages');
const nextStep = document.getElementById('next-step');


const orderId = document.getElementById('order-id');
const pickupLocationElement = document.getElementById('pickup-location');
const destinationLocationElement = document.getElementById('destination-location');
const orderPrice = document.getElementById('order-price');
const orderStatus = document.getElementById('order-status');

// 初始化页面
document.addEventListener('DOMContentLoaded', () => {
    try {
        console.log('页面初始化开始...');
        
        baseUrl = 'http://localhost:8082';

        // 初始化地图
        initMap();
        console.log('地图初始化完成');
        
        // 更新时间
        updateTime();
        setInterval(updateTime, 1000);
        console.log('时间更新器设置完成');
        
        // 添加事件监听器
        loginBtn.addEventListener('click', login);
        offlineBtn.addEventListener('click', goOffline);
        checkConnectionBtn.addEventListener('click', checkConnection);

        // 添加初始状态信息
        addStatusMessage('系统已启动，等待连接...', 'info');
        
        // 连接WebSocket
        connect();
        
        // 检查连接状态
        checkConnection();
        
        console.log('页面初始化完成');
    } catch (error) {
        console.error('页面初始化错误:', error);
        addStatusMessage('页面初始化失败: ' + error.message, 'error');
    }

    // 在页面关闭时断开连接
    window.addEventListener('beforeunload', function() {
        goOffline();
    });
});

var stompClient = null;

// 连接到 WebSocket 服务器
function connect() {
    console.log('正在连接WebSocket服务器...');
    try {
        var socket = new SockJS(`${baseUrl}/DriverWebSocket`);
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
                addStatusMessage('WebSocket连接已建立');
                
                // 订阅消息通道
                try {
                    stompClient.subscribe('/topic/driver', function(data) {
                        console.log('收到司机信息更新:', data);
                        try {
                            driver = JSON.parse(data.body);
                            updateDriverInfo();
                        } catch (e) {
                            console.error('解析司机信息失败:', e);
                        }
                    });

                    stompClient.subscribe('/topic/order', function(data) {
                        console.log('收到订单状态更新:', data);
                        try {
                            order = JSON.parse(data.body);
                            updateOrderInfo();
                        } catch (e) {
                            console.error('解析订单信息失败:', e);
                        }
                    });
                    
                    console.log('所有消息通道订阅成功');
                } catch (e) {
                    console.error('订阅消息通道失败:', e);
                    addStatusMessage('订阅消息通道失败');
                }
            },
            // 连接错误回调
            function(error) {
                console.error('WebSocket连接错误:', error);
                addStatusMessage('WebSocket连接失败，5秒后重试...');
                setTimeout(connect, 5000);
            }
        );
    } catch (e) {
        console.error('创建WebSocket连接失败:', e);
        addStatusMessage('创建WebSocket连接失败，5秒后重试...');
        setTimeout(connect, 5000);
    }
}


// 检查连接
function checkConnection() {
    fetch(`${baseUrl}/driver/test`)
        .then(response => {
            if (response.ok) {
                connectionStatus.textContent = '服务正常';
                connectionStatus.className = 'connected';
                addStatusMessage('服务连接正常');
            } else {
                throw new Error('服务不可用');
            }
        })
        .catch(error => {
            connectionStatus.textContent = '连接失败';
            connectionStatus.className = 'disconnected';
            addStatusMessage('服务连接失败');
            console.error('连接检查错误:', error);
        });
}

// 初始化地图
function initMap() {
    mapContext = mapCanvas.getContext('2d');
    drawMap();
}

// 绘制地图
function drawMap() {
    // 清空地图
    mapContext.clearRect(0, 0, mapCanvas.width, mapCanvas.height);
    
    // 绘制网格
    mapContext.strokeStyle = '#ddd';
    mapContext.lineWidth = 1;
    
    for (let i = 0; i <= gridSize; i++) {
        // 垂直线
        mapContext.beginPath();
        mapContext.moveTo(i * cellSize, 0);
        mapContext.lineTo(i * cellSize, gridSize * cellSize);
        mapContext.stroke();
        
        // 水平线
        mapContext.beginPath();
        mapContext.moveTo(0, i * cellSize);
        mapContext.lineTo(gridSize * cellSize, i * cellSize);
        mapContext.stroke();
    }
    
    // 如果有上车点，绘制上车点标记
    if (order && order.startLocation) {
        mapContext.fillStyle = '#4CAF50'; // 绿色表示乘客上车点
        mapContext.beginPath();
        mapContext.arc(
            (order.startLocation.x + 0.5) * cellSize,
            (order.startLocation.y + 0.5) * cellSize,
            cellSize / 4,
            0,
            Math.PI * 2
        );
        mapContext.fill();
        
        // 添加标签"起点"
        mapContext.fillStyle = '#000';
        mapContext.font = '14px Arial';
        mapContext.textAlign = 'center';
        mapContext.fillText('起点', (order.startLocation.x + 0.5) * cellSize, (order.startLocation.y + 0.5) * cellSize - 20);
    }
    
    // 如果有目的地，绘制目的地标记
    if (order && order.endLocation) {
        mapContext.fillStyle = '#FF5722'; // 红橙色表示目的地
        mapContext.beginPath();
        mapContext.arc(
            (order.endLocation.x + 0.5) * cellSize,
            (order.endLocation.y + 0.5) * cellSize,
            cellSize / 4,
            0,
            Math.PI * 2
        );
        mapContext.fill();
        
        // 添加标签"终点"
        mapContext.fillStyle = '#000';
        mapContext.font = '14px Arial';
        mapContext.textAlign = 'center';
        mapContext.fillText('终点', (order.endLocation.x + 0.5) * cellSize, (order.endLocation.y + 0.5) * cellSize - 20);
    }
    
    // 绘制司机位置
    if (driver && driver.currentLocation) {
        mapContext.fillStyle = '#2196F3'; // 蓝色表示司机
        mapContext.beginPath();
        mapContext.arc(
            (driver.currentLocation.x + 0.5) * cellSize,
            (driver.currentLocation.y + 0.5) * cellSize,
            cellSize / 3,
            0,
            Math.PI * 2
        );
        mapContext.fill();
        
        // 添加标签"司机"
        mapContext.fillStyle = '#000';
        mapContext.font = '14px Arial';
        mapContext.textAlign = 'center';
        mapContext.fillText('司机', (driver.currentLocation.x + 0.5) * cellSize, (driver.currentLocation.y + 0.5) * cellSize - 20);
    }
}

// 更新时间
function updateTime() {
    const now = new Date();
    currentTimeElement.textContent = now.toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    });
}

// 更新司机信息显示
function updateDriverInfo() {
    if (!driver) return;
    
    driverNameElement.textContent = driver.name || '未登录';
    driverStatusElement.textContent = driver.status || '离线';
    completedOrdersElement.textContent = driver.completedOrders || '0';
    totalEarningsElement.textContent = driver.totalEarnings ? driver.totalEarnings.toFixed(2) : '0.00';
    
    if (driver.currentLocation) {
        currentLocationElement.textContent = `(${driver.currentLocation.x}, ${driver.currentLocation.y})`;
    } else {
        currentLocationElement.textContent = '未知';
    }
    
    // 重新绘制地图以更新司机位置
    drawMap();
}

// 更新订单信息显示
function updateOrderInfo() {
    if (!order) return;
    
    orderId.textContent = order.orderId || '无';
    pickupLocationElement.textContent = order.startLocation ? 
        `(${order.startLocation.x}, ${order.startLocation.y})` : '无';
    destinationLocationElement.textContent = order.endLocation ? 
        `(${order.endLocation.x}, ${order.endLocation.y})` : '无';
    orderPrice.textContent = order.price ? order.price.toFixed(2) : '0.00';
    orderStatus.textContent = order.status || '无';
    
    // 重新绘制地图以更新订单位置
    drawMap();
}


// 登录处理
async function login() {
    const name = nameInput.value.trim();
    if (!name) {
        addStatusMessage('请输入姓名', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${baseUrl}/driver/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name })
        });
        
        if (!response.ok) {
            throw new Error('登录失败');
        }
        
        const data = await response.json();
        driver = data;
        
        // 更新UI
        updateDriverInfo();
        
        // 隐藏登录面板，显示其他面板
        loginPanel.classList.add('hidden');
        offlineBtn.disabled = false;
        
        addStatusMessage(`欢迎，${driver.name}！`);
    } catch (error) {
        console.error('登录错误:', error);
        addStatusMessage('登录失败，请重试', 'error');
    }
}

// 下线处理
async function goOffline() {
    if (!driver || !driver.name) return;
    
    try {
        const response = await fetch(`${baseUrl}/driver/${driver.name}/offline`, {
            method: 'POST'
        });
        
        if (!response.ok) {
            throw new Error('下线失败');
        }
        
        // 更新UI
        driver.status = '下线';
        updateDriverInfo();
        
        // 显示登录面板，隐藏其他面板
        loginPanel.classList.remove('hidden');
        offlineBtn.disabled = true;
        orderPanel.classList.add('hidden');
        
        addStatusMessage('您已下线');
    } catch (error) {
        console.error('下线错误:', error);
        addStatusMessage('下线失败，请重试', 'error');
    }
}

// 添加状态消息
function addStatusMessage(message, type = 'info') {
    const messageElement = document.createElement('div');
    messageElement.className = `status-message ${type}`;
    
    const timeSpan = document.createElement('span');
    timeSpan.className = 'time';
    timeSpan.textContent = new Date().toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
    
    const messageSpan = document.createElement('span');
    messageSpan.textContent = message;
    
    messageElement.appendChild(timeSpan);
    messageElement.appendChild(messageSpan);
    
    statusMessages.appendChild(messageElement);
    statusMessages.scrollTop = statusMessages.scrollHeight;
    
    // 5秒后自动删除
    setTimeout(() => {
        messageElement.remove();
    }, 5000);
}










