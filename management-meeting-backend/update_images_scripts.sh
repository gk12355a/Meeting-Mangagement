#!/bin/bash

# 1. Nhận các tham số truyền vào từ Jenkins
# Thứ tự: $1=TênService, $2=VersionTag, $3=MôiTrường(dev/prod), $4=ThưMụcChứaYAML
SERVICE_NAME=$1
VERSION=$2
ENV_NAME=$3
YAML_DIR=$4

# 2. Cấu hình Docker Hub User (phải khớp với Jenkinsfile)
DOCKER_USER="gk123a"

# 3. Tạo chuỗi Image Tag mới
# Ví dụ kết quả: v1.0.0-dev
NEW_TAG="${VERSION}-${ENV_NAME}"

# Tạo chuỗi Image đầy đủ để thay thế vào file YAML
# Ví dụ: gk123a/meeting-management-backend:v1.0.0-dev
NEW_FULL_IMAGE="${DOCKER_USER}/${SERVICE_NAME}:${NEW_TAG}"

echo "-------------------------------------------------"
echo "Script: Update Image in Kubernetes Manifest"
echo "Target File Path: $YAML_DIR/*.yaml"
echo "Service Name    : $SERVICE_NAME"
echo "New Image Tag   : $NEW_FULL_IMAGE"
echo "-------------------------------------------------"

# 4. Thực hiện thay thế bằng lệnh sed
# Giải thích lệnh sed:
# -i : chỉnh sửa trực tiếp trên file (in-place)
# s|...|...|g : thay thế nội dung (dùng dấu | làm phân cách để tránh lỗi với dấu / trong tên ảnh)
# Regex: tìm dòng bắt đầu bằng 'image:', có chứa tên service, và thay thế toàn bộ phần sau 'image:'

# Kiểm tra hệ điều hành để chạy lệnh sed phù hợp
if [[ "$OSTYPE" == "darwin"* ]]; then
  # Dành cho macOS (nếu bạn chạy test local)
  sed -i '' "s|image: .*/${SERVICE_NAME}:.*|image: ${NEW_FULL_IMAGE}|g" ${YAML_DIR}/*.yaml
else
  # Dành cho Linux (Môi trường Jenkins)
  sed -i "s|image: .*/${SERVICE_NAME}:.*|image: ${NEW_FULL_IMAGE}|g" ${YAML_DIR}/*.yaml
fi

# 5. Kiểm tra kết quả
if [ $? -eq 0 ]; then
  echo "✅ Success: Image updated to [${NEW_FULL_IMAGE}]"
else
  echo "❌ Error: Failed to update image tag."
  exit 1
fi