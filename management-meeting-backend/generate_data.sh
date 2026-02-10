#!/bin/bash

# Kiểm tra xem token có được cung cấp không
if [ -z "$1" ]; then
  echo "Usage: $0 <ACCESS_TOKEN>"
  echo "Vui lòng cung cấp Admin Access Token để chạy script."
  exit 1
fi

TOKEN="$1"
BASE_URL="http://localhost:8080/api/v1"

echo "============================================="
echo "Bắt đầu tạo 100 thiết bị (Devices)..."
echo "============================================="

for i in {1..100}
do
  # Tạo dữ liệu thiết bị
  DEVICE_NAME="Device Auto $i"
  DEVICE_DESC="Generated automatic device number $i"
  
  # Tạo file JSON tạm thời cho request body của Device
  cat <<EOF > device_request.json
{
  "name": "${DEVICE_NAME}",
  "description": "${DEVICE_DESC}",
  "status": "AVAILABLE"
}
EOF

  echo "Creating device $i ($DEVICE_NAME)..."

  # Gửi request bằng curl (Multipart/form-data)
  # Lưu ý: DeviceController yêu cầu request part là "request"
  response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -F "request=@device_request.json;type=application/json" \
    "$BASE_URL/devices")

  if [ "$response" -eq 200 ] || [ "$response" -eq 201 ]; then
    echo " -> Success ($response)"
  else
    echo " -> Failed ($response)"
  fi

done

# Xóa file tạm
rm device_request.json


echo "============================================="
echo "Bắt đầu tạo 50 phòng họp (Rooms)..."
echo "============================================="

for i in {1..50}
do
  # Tạo dữ liệu phòng
  ROOM_NAME="Room Auto $i"
  CAPACITY=$(( ( RANDOM % 20 ) + 5 )) # Random capacity từ 5 đến 24
  FLOOR=$(( ( RANDOM % 10 ) + 1 ))    # Random floor từ 1 đến 10
  BUILDING="CMC Tower"
  LOCATION="Hanoi, Floor $FLOOR"
  
  # Tạo nội dung JSON cho Room
  # RoomController có endpoint nhận JSON trực tiếp
  cat <<EOF > room_request.json
{
  "name": "${ROOM_NAME}",
  "capacity": ${CAPACITY},
  "location": "${LOCATION}",
  "buildingName": "${BUILDING}",
  "floor": ${FLOOR},
  "status": "AVAILABLE",
  "requiresApproval": false
}
EOF

  echo "Creating room $i ($ROOM_NAME)..."

  # Gửi request bằng curl (JSON)
  response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d @room_request.json \
    "$BASE_URL/rooms")

  if [ "$response" -eq 200 ] || [ "$response" -eq 201 ]; then
     echo " -> Success ($response)"
  else
     echo " -> Failed ($response)"
  fi

done

# Xóa file tạm
rm room_request.json

echo "============================================="
echo "Hoàn thành tạo dữ liệu mẫu."
echo "============================================="
