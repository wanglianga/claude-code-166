# 前端镜像：Node 构建 + Nginx 托管
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json ./
RUN npm install --registry https://registry.npmmirror.com
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
