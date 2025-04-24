// Biến toàn cục để quản lý kết nối và trạng thái chat
let stompClient = null;          // Client WebSocket STOMP
let currentChat = null;          // Cuộc trò chuyện hiện tại
let currentChatType = null;      // Loại chat: 'private' hoặc 'group'
let onlineUsers = new Set();     // Danh sách người dùng đang online

/**
 * Thiết lập kết nối WebSocket và đăng ký các kênh nhận tin nhắn
 */
function connect() {
   // const socket = new SockJS('/ws');
    const socket = new SockJS(window.location.origin + '/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);

        // Đăng ký nhận tin nhắn riêng tư
        stompClient.subscribe('/user/' + username + '/private', function(message) {
            const messageData = JSON.parse(message.body);
            showMessage(messageData);
        });

        // Đăng ký nhận các sự kiện hệ thống
        stompClient.subscribe('/topic/public', function(message) {
            const notification = JSON.parse(message.body);
            if (notification.type === 'JOIN') {
                addOnlineUser(notification.sender);
            } else if (notification.type === 'LEAVE') {
                removeOnlineUser(notification.sender);
            } else if (notification.type === 'USERS_LIST') {
                // Cập nhật danh sách người dùng online
                onlineUsers.clear();
                const usersList = notification.content.split(',');
                usersList.forEach(user => {
                    if (user !== username) {
                        onlineUsers.add(user);
                    }
                });
                updateOnlineUsersList();
            } else if (notification.type === 'GROUP_CREATED') {
                // Xử lý thông báo tạo nhóm mới
                const { group, members } = JSON.parse(notification.content);
                if (members.includes(username)) {
                    addChatToList(group);
                }
            }
        });

        // Thông báo tham gia hệ thống
        stompClient.send("/app/chat.addUser",
            {},
            JSON.stringify({sender: username, type: 'JOIN'})
        );

        // Tải danh sách chat có sẵn
        loadChats();
    });
}

/**
 * Tải danh sách các cuộc trò chuyện từ server
 */
function loadChats() {
    fetch('/api/chats')
        .then(response => response.json())
        .then(chats => {
            const chatList = document.getElementById('chat-list');
            chatList.innerHTML = '';
            chats.forEach(chat => {
                addChatToList(chat);
            });
        });
}

/**
 * Thêm một cuộc trò chuyện vào danh sách
 * @param chat Thông tin cuộc trò chuyện
 */
function addChatToList(chat) {
    const chatList = document.getElementById('chat-list');
    const chatItem = document.createElement('div');
    chatItem.className = 'chat-list-item d-flex align-items-center';
    chatItem.onclick = () => selectChat(chat);

    const avatar = document.createElement('div');
    avatar.className = 'user-avatar me-2';
    avatar.textContent = chat.name.charAt(0).toUpperCase();

    const nameDiv = document.createElement('div');
    nameDiv.textContent = chat.name;

    chatItem.appendChild(avatar);
    chatItem.appendChild(nameDiv);
    chatList.appendChild(chatItem);
}

/**
 * Chọn một cuộc trò chuyện để hiển thị
 * @param chat Thông tin cuộc trò chuyện được chọn
 */
function selectChat(chat) {
    // Hủy đăng ký kênh nhóm cũ nếu có
    if (currentChat?.type === 'group' && window.currentGroupSubscription) {
        window.currentGroupSubscription.unsubscribe();
    }

    currentChat = chat;
    currentChatType = chat.type;
    
    // Cập nhật giao diện
    document.getElementById('current-chat-name').textContent = chat.name;
    document.getElementById('current-chat-avatar').textContent = chat.name.charAt(0).toUpperCase();
    
    // Kích hoạt ô nhập tin nhắn
    document.getElementById('message').disabled = false;
    document.querySelector('#message-form button').disabled = false;

    // Đăng ký nhận tin nhắn nhóm nếu là chat nhóm
    if (chat.type === 'group') {
        window.currentGroupSubscription = stompClient.subscribe(
            '/topic/group/' + chat.id,
            function(message) {
                const messageData = JSON.parse(message.body);
                showMessage(messageData);
            }
        );
    }

    // Tải lịch sử chat
    loadChatHistory(chat);

    // Cập nhật trạng thái active trong danh sách
    document.querySelectorAll('.chat-list-item').forEach(item => {
        item.classList.remove('active');
        if (item.querySelector('div').textContent === chat.name) {
            item.classList.add('active');
        }
    });
}

/**
 * Tải lịch sử tin nhắn của một cuộc trò chuyện
 * @param chat Thông tin cuộc trò chuyện
 */
function loadChatHistory(chat) {
    const url = chat.type === 'group' 
        ? `/api/messages/group/${chat.id}`
        : `/api/messages/private/${chat.id}`;
    
    console.log('Loading chat history from:', url);
        
    fetch(url)
        .then(response => {
            console.log('Response status:', response.status);
            return response.json();
        })
        .then(messages => {
            console.log('Received messages:', messages);
            const chatMessages = document.getElementById('chat-messages');
            chatMessages.innerHTML = '';
            if (Array.isArray(messages)) {
                messages.forEach(message => {
                    console.log('Processing message:', message);
                    showMessage(message);
                });
            } else {
                console.error('Received non-array messages:', messages);
            }
        })
        .catch(error => {
            console.error('Error loading chat history:', error);
        });
}

/**
 * Gửi tin nhắn mới
 * @param event Form submit event
 */
function sendMessage(event) {
    event.preventDefault();
    const messageInput = document.getElementById('message');
    const messageContent = messageInput.value.trim();
    
    if (messageContent && stompClient && currentChat) {
        const chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT'
        };

        if (currentChatType === 'group') {
            chatMessage.groupId = currentChat.id;
            stompClient.send("/app/chat.group", {}, JSON.stringify(chatMessage));
        } else {
            chatMessage.receiver = currentChat.id;
            stompClient.send("/app/chat.private", {}, JSON.stringify(chatMessage));
        }

        messageInput.value = '';
    }
}

/**
 * Hiển thị tin nhắn trong khung chat
 * @param message Thông tin tin nhắn
 */
function showMessage(message) {
    console.log('Showing message:', message);
    
    // Bỏ qua nếu không phải tin nhắn chat hoặc chưa chọn chat
    if (!currentChat || (message.type && message.type !== 'CHAT')) {
        console.log('Skipping message - not a chat message or no current chat');
        return;
    }
    
    // Kiểm tra tin nhắn có thuộc về cuộc trò chuyện hiện tại không
    const isPrivateChat = (currentChatType === 'private' && (
        (message.sender === username && message.receiver === currentChat.id) ||
        (message.receiver === username && message.sender === currentChat.id)
    ));
    const isGroupChat = (currentChatType === 'group' && message.groupId === currentChat.id);
    
    console.log('Message validation:', {
        isPrivateChat,
        isGroupChat,
        currentChatType,
        currentChatId: currentChat.id
    });
    
    if (isPrivateChat || isGroupChat) {
        const messagesList = document.getElementById('chat-messages');
        const messageElement = document.createElement('div');
        messageElement.classList.add('message');
        
        const senderName = message.sender;
        if (senderName === username) {
            messageElement.classList.add('sent');
        } else {
            messageElement.classList.add('received');
        }

        const content = document.createElement('div');
        content.textContent = message.content;
        
        const metadata = document.createElement('div');
        metadata.classList.add('message-time');
        const timestamp = message.timestamp ? new Date(message.timestamp) : new Date();
        metadata.textContent = senderName + ' · ' + formatTime(timestamp);
        
        messageElement.appendChild(content);
        messageElement.appendChild(metadata);
        messagesList.appendChild(messageElement);
        messagesList.scrollTop = messagesList.scrollHeight;
    } else {
        console.log('Message does not belong to current chat');
    }
}

/**
 * Bắt đầu cuộc trò chuyện mới với một người dùng
 * @param otherUsername Username của người dùng muốn chat cùng
 */
function startChat(otherUsername) {
    if (otherUsername && otherUsername !== username) {
        const chat = {
            id: otherUsername,
            name: otherUsername,
            type: 'private'
        };
        addChatToList(chat);
        selectChat(chat);
    }
}

/**
 * Tạo nhóm chat mới
 */
function createNewGroup() {
    const groupName = document.getElementById('group-name').value.trim();
    const selectedMembers = Array.from(document.querySelectorAll('#group-members-select input:checked'))
        .map(checkbox => checkbox.value);
    
    if (groupName && selectedMembers.length > 0) {
        fetch('/api/group/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: groupName,
                members: [...selectedMembers]
            })
        })
        .then(response => response.json())
        .then(group => {
            addChatToList(group);
            selectChat(group);
            const modal = bootstrap.Modal.getInstance(document.getElementById('newGroupModal'));
            modal.hide();
        })
        .catch(error => {
            console.error('Error creating group:', error);
            alert('Failed to create group. Please try again.');
        });
    }
}

/**
 * Thêm người dùng vào danh sách online
 * @param user Username của người dùng
 */
function addOnlineUser(user) {
    if (user !== username && !onlineUsers.has(user)) {
        onlineUsers.add(user);
        updateOnlineUsersList();
    }
}

/**
 * Xóa người dùng khỏi danh sách online
 * @param user Username của người dùng
 */
function removeOnlineUser(user) {
    if (onlineUsers.has(user)) {
        onlineUsers.delete(user);
        updateOnlineUsersList();
    }
}

/**
 * Cập nhật giao diện hiển thị danh sách người dùng online
 */
function updateOnlineUsersList() {
    const onlineUsersList = document.getElementById('online-users-list');
    const onlineUsersSelect = document.getElementById('online-users-select');
    const groupMembersSelect = document.getElementById('group-members-select');
    
    onlineUsersList.innerHTML = '';
    onlineUsersSelect.innerHTML = '';
    groupMembersSelect.innerHTML = '';

    Array.from(onlineUsers).sort().forEach(user => {
        // Thêm vào danh sách bên sidebar
        const userItem = document.createElement('div');
        userItem.className = 'online-user-item';
        userItem.innerHTML = `<span class="online-badge"></span>${user}`;
        userItem.onclick = () => startChat(user);
        onlineUsersList.appendChild(userItem);

        // Thêm vào modal chat mới
        const userSelectItem = document.createElement('div');
        userSelectItem.className = 'online-user-item';
        userSelectItem.innerHTML = `<span class="online-badge"></span>${user}`;
        userSelectItem.onclick = () => {
            startChat(user);
            $('#newChatModal').modal('hide');
        };
        onlineUsersSelect.appendChild(userSelectItem);

        // Thêm vào phần chọn thành viên nhóm
        const memberCheckbox = document.createElement('div');
        memberCheckbox.className = 'form-check';
        memberCheckbox.innerHTML = `
            <input class="form-check-input" type="checkbox" value="${user}" id="member-${user}">
            <label class="form-check-label" for="member-${user}">${user}</label>
        `;
        groupMembersSelect.appendChild(memberCheckbox);
    });
}

/**
 * Format thời gian hiển thị
 * @param timestamp Thời gian cần format
 * @return Chuỗi thời gian đã format
 */
function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/**
 * Đăng xuất khỏi hệ thống
 */
function logout() {
    window.location.href = '/logout';
}

// Đăng ký các event listeners
document.getElementById('message-form').addEventListener('submit', sendMessage);
window.addEventListener('load', connect);