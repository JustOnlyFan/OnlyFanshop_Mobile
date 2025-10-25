# Test Customer Name Display in Chat Room

## 🎯 Mục tiêu
Kiểm tra xem tên customer có hiển thị đúng trong chat room của admin không.

## 📋 Test Cases

### ✅ Test Case 1: Admin vào chat room
**Steps:**
1. Login với admin account
2. Vào Chat Management
3. Click vào một customer chat
4. Kiểm tra title của chat room

**Expected Result:**
- Title hiển thị: `{CustomerName} Chat`
- Ví dụ: `NTT Chat` thay vì `OnlyFanShop Chat`

### ✅ Test Case 2: Customer vào chat room  
**Steps:**
1. Login với customer account
2. Vào Profile → Chat with Admin
3. Kiểm tra title của chat room

**Expected Result:**
- Title hiển thị: `OnlyFanShop Chat`

## 🔍 Debug Information

### Room ID Format:
- Format: `chatRoom_{username}_{userId}`
- Example: `chatRoom_NTT_4`

### User Role Detection:
- Admin: `ADMIN`
- Customer: `CUSTOMER`

### Debug Logs:
```
User Role: ADMIN, Room ID: chatRoom_NTT_4
Extracted customer name: NTT
```

## 🚀 Implementation

### ✅ AppPreferences.getUserRole()
- Lấy role từ SharedPreferences
- Fallback từ MyAppPrefs nếu không tìm thấy

### ✅ extractCustomerNameFromRoomId()
- Parse room ID để lấy username
- Handle error cases gracefully

### ✅ Chat Title Logic
- Admin: `{CustomerName} Chat`
- Customer: `OnlyFanShop Chat`

## 📱 Test Results

**Status:** ✅ Ready for testing
**Next:** Test với mobile app để verify customer name display

