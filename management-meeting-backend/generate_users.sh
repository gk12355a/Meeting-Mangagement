#!/bin/bash

# Kiểm tra xem token có được cung cấp không
if [ -z "$1" ]; then
  echo "Usage: $0 <ACCESS_TOKEN>"
  echo "Vui lòng cung cấp Admin Access Token để chạy script."
  exit 1
fi

TOKEN="$1"
BASE_URL="http://localhost:8080/api/v1/admin/users"

echo "Bắt đầu tạo 200 user..."

for i in {1..200}
do
  # Tạo dữ liệu ngẫu nhiên
  RANDOM_STR=$(date +%s%N)
  USERNAME="user_${i}_${RANDOM_STR}@example.com"
  FULLNAME="Generated User $i"
  PASSWORD="password123"

  # Tạo file JSON tạm thời cho request body
  cat <<EOF > user_request.json
{
  "fullName": "${FULLNAME}",
  "username": "${USERNAME}",
  "password": "${PASSWORD}",
  "roles": ["ROLE_USER"]
}
EOF

  echo "Creating user $i ($USERNAME)..."

  # Gửi request bằng curl
  response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -F "request=@user_request.json;type=application/json" \
    "$BASE_URL")

  if [ "$response" -eq 200 ] || [ "$response" -eq 201 ]; then
    echo " -> Success ($response)"
  else
    echo " -> Failed ($response)"
  fi

done

# Xóa file tạm
rm user_request.json

echo "Hoàn thành."
