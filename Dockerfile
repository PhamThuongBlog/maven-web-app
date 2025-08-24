# Dockerfile mẫu cho ứng dụng Maven .war

# 1. Chọn base image Tomcat
FROM tomcat:9.0-jdk11

# 2. Xóa file sample default của Tomcat (nếu cần)
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# 3. Copy file WAR từ workspace vào thư mục webapps của Tomcat
# Lưu ý: file .war cần có tên ROOT.war để Tomcat chạy mặc định trên /
COPY target/maven-web-app.war /usr/local/tomcat/webapps/ROOT.war

# 4. Expose port Tomcat
EXPOSE 8085

# 5. Command mặc định để chạy Tomcat
CMD ["catalina.sh", "run"]
