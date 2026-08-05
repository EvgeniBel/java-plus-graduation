# Explore With Me (EWM)

**Explore With Me** — это платформа для поиска и организации событий. Пользователи могут создавать события, участвовать в них, оставлять комментарии и отслеживать статистику просмотров.

---

## 📋 Содержание

- [Архитектура](#архитектура)
- [Сервисы](#сервисы)
- [Взаимодействие между сервисами](#взаимодействие-между-сервисами)
- [Конфигурация](#конфигурация)
- [Внутренний API](#внутренний-api)
- [Внешний API](#внешний-api)
- [Модуль комментариев](#модуль-комментариев)
- [Тестирование](#тестирование)
- [Запуск](#запуск)
- [Технологии](#технологии)

---

## Архитектура

Проект построен на **микросервисной архитектуре** с использованием **Spring Cloud** и **Eureka** для обнаружения сервисов. Каждый сервис имеет свою базу данных PostgreSQL и независимо масштабируется.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          API Gateway (порт 8080)                           │
│                         Spring Cloud Gateway                               │
└────────────────────────────┬────────────────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┬────────────────────┐
        │                    │                    │                    │
        ▼                    ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│    USER       │   │    EVENT      │   │   REQUEST     │   │   COMMENT     │
│   SERVICE     │   │   SERVICE     │   │   SERVICE     │   │   SERVICE     │
│               │   │               │   │               │   │               │
│ PostgreSQL    │   │ PostgreSQL    │   │ PostgreSQL    │   │ PostgreSQL    │
│    :5438      │   │    :5435      │   │    :5437      │   │    :5436      │
└───────────────┘   └───────────────┘   └───────────────┘   └───────────────┘
        │                    │                    │                    │
        └────────────────────┴────────────────────┴────────────────────┘
                                        │
                                        ▼
                             ┌───────────────────┐
                             │   STATS SERVICE   │
                             │                   │
                             │   PostgreSQL      │
                             │     :5434         │
                             └───────────────────┘
```

---

## Сервисы

| Сервис | Описание | Порт | База данных       |
|--------|----------|------|-------------------|
| **Gateway Server** | Единая точка входа, маршрутизация запросов | 8080 | -                 |
| **Eureka Server** | Service Discovery | 8761 | -                 |
| **Config Server** | Централизованное управление конфигурациями | 0 (random) | -                 |
| **User Service** | Управление пользователями | 0 (random) | PostgreSQL :5438  |
| **Event Service** | Управление событиями, категориями, подборками | 0 (random) | PostgreSQL :5435  |
| **Request Service** | Управление запросами на участие | 0 (random) | PostgreSQL :5437  |
| **Comment Service** | Управление комментариями | 0 (random) | PostgreSQL :5436  |
| **Stats Service** | Сбор и хранение статистики просмотров | 0 (random) | PostgreSQL :5434  |

---

## Взаимодействие между сервисами

### Схема взаимодействия

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FEIGN CLIENTS                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                 │
│  │  UserClient  │    │ EventClient  │    │ RequestClient│                 │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                 │
│         │                   │                   │                          │
│         ▼                   ▼                   ▼                          │
│  ┌──────────────────────────────────────────────────────┐                 │
│  │              Event Service                          │                 │
│  │  - Проверка пользователя через UserClient           │                 │
│  │  - Получение событий через EventClient (извне)     │                 │
│  │  - Обновление статусов запросов через RequestClient │                 │
│  │  - Отправка статистики через StatClient            │                 │
│  └──────────────────────────────────────────────────────┘                 │
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐                                     │
│  │ RequestClient│    │  UserClient  │                                     │
│  └──────┬───────┘    └──────┬───────┘                                     │
│         │                   │                                              │
│         ▼                   ▼                                              │
│  ┌──────────────────────────────────────────────────────┐                 │
│  │              Request Service                         │                 │
│  │  - Проверка пользователя через UserClient           │                 │
│  │  - Проверка события через EventClient              │                 │
│  └──────────────────────────────────────────────────────┘                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Типы взаимодействия

| От кого | Кому | Способ | Описание |
|---------|------|--------|----------|
| Event Service | User Service | **Feign** | Проверка существования пользователя, получение данных |
| Event Service | Request Service | **Feign** | Создание/обновление запросов, получение статистики |
| Event Service | Stats Service | **Feign** | Отправка и получение статистики просмотров |
| Request Service | User Service | **Feign** | Проверка существования пользователя |
| Request Service | Event Service | **Feign** | Проверка существования события, получение данных |
| Gateway → Services | REST | **HTTP** | Маршрутизация запросов от клиентов |

---

## Конфигурация

Все конфигурации хранятся в **Config Server** и подтягиваются сервисами при запуске.

### Структура конфигураций

```
infra/config-server/src/main/resources/
├── application.yaml                                   # Общие настройки
└──  config/ 
      ├── core 
      ├    ├── comment-service/comment-service.yaml      # Настройки Comment Service
      ├    ├── event-service/event-service.yaml          # Настройки Event Service
      ├    ├── request-service/request-service.yaml      # Настройки Request Service
      ├    └── user-service/user-service.yaml            # Настройки User Service
      ├
      ├──infra/gateway-serverateway-server.yaml        # Настройки Gateway
      └──stats/stats-server/stats-server.yaml          # Настройки Stats Service
    
```

### Основные настройки

#### Базы данных
```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5435/ewm-event-service
    username: ewm
    password: ewm
```

#### Eureka
```yaml
eureka:
  client:
    enabled: true
    register-with-eureka: true
    fetch-registry: true
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

#### Gateway
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: event_service_route
          uri: lb://EVENT-SERVICE
          predicates:
            - Path=/admin/events/**, /events/**, /users/{userId}/events/**
```

---

## Внутренний API

Внутренний API используется для взаимодействия между микросервисами через Feign клиенты. Все внутренние эндпоинты имеют префикс `/internal/**`.

### User Service (внутренние)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/internal/users/{userId}/exists` | Проверка существования пользователя |
| `GET` | `/internal/users/{userId}` | Получение данных пользователя |
| `POST` | `/internal/users/validate` | Валидация пользователя |

### Request Service (внутренние)

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/internal/requests/{requestId}/status` | Обновление статуса запроса |

### Comment Service (внутренние)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/internal/comments/{commentId}/exists` | Проверка существования комментария |
| `GET` | `/internal/comments/{commentId}/status` | Получение статуса комментария |

---

## Внешний API

Прием внешних запросов осуществляется через шлюз по адресу: `http://localhost:8080`

**Контракты и схемы взаимодействия доступны по адресу:**

- **[Интерфейс основного приложения (EWM)](https://github.com/EvgeniBel/java-plus-graduation/blob/microservices/postman/ewm-main-service-spec.json)**
- **[Интерфейс модуля статистики](https://github.com/EvgeniBel/java-plus-graduation/blob/microservices/postman/ewm-stats-service-spec.json)**

### Основные эндпоинты

#### Пользователи (Admin)
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/admin/users` | Создание пользователя |
| `GET` | `/admin/users` | Получение списка пользователей |
| `DELETE` | `/admin/users/{userId}` | Удаление пользователя |

#### События (Public)
| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/events` | Получение событий с фильтрацией |
| `GET` | `/events/{eventId}` | Получение события по ID |

#### События (Private)
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/users/{userId}/events` | Создание события |
| `PATCH` | `/users/{userId}/events/{eventId}` | Обновление события |
| `PATCH` | `/users/{userId}/events/{eventId}/requests` | Обновление статусов запросов |

#### Запросы на участие
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/users/{userId}/requests?eventId={eventId}` | Создание запроса |
| `PATCH` | `/users/{userId}/requests/{requestId}/cancel` | Отмена запроса |
| `GET` | `/users/{userId}/requests` | Получение запросов пользователя |

#### Комментарии (Private)
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/users/{userId}/comments?eventId={eventId}` | Создание комментария |
| `PATCH` | `/users/{userId}/comments/{commentId}` | Обновление комментария |
| `DELETE` | `/users/{userId}/comments/{commentId}` | Удаление комментария |

#### Комментарии (Admin)
| Метод | Путь | Описание |
|-------|------|----------|
| `PATCH` | `/admin/comments/{commentId}/moderate` | Модерация комментария |
| `DELETE` | `/admin/comments/{commentId}` | Удаление комментария |

---

## Модуль комментариев

Модуль управления комментариями пользователей к событиям.

### Обзор

Функциональность разделена на три зоны доступа:

- **Private API**: Доступно только автору комментария (создание, редактирование, удаление)
- **Public API**: Доступно всем пользователям (просмотр одобренных комментариев)
- **Admin API**: Доступно администраторам (модерация статусов, просмотр всех комментариев, принудительное удаление)

### Статусы комментариев

| Статус | Описание | Видимость в Public API |
|--------|----------|------------------------|
| `PENDING` | Ожидает модерации (статус по умолчанию) | Скрыт |
| `APPROVED` | Одобрен администратором | Видим |
| `REJECTED` | Отклонен администратором | Скрыт |

### API Endpoints

#### Private API
**Базовый путь:** `/users/{userId}/comments`

| Метод | Путь | Описание | Код ответа |
|-------|------|----------|------------|
| `POST` | `/` | Создание нового комментария | `201 Created` |
| `PATCH` | `/{commentId}` | Обновление текста комментария | `200 OK` |
| `DELETE` | `/{commentId}` | Удаление комментария автором | `204 No Content` |

#### Public API
**Базовый путь:** `/events/{eventId}/comments`

| Метод | Путь | Описание | Код ответа |
|-------|------|----------|------------|
| `GET` | `/` | Получение списка одобренных комментариев | `200 OK` |

#### Admin API
**Базовый путь:** `/admin/comments`

| Метод | Путь | Описание | Код ответа |
|-------|------|----------|------------|
| `PATCH` | `/{commentId}/moderate` | Изменение статуса комментария | `200 OK` |
| `GET` | `/events/{eventId}` | Получение комментариев события | `200 OK` |
| `DELETE` | `/{commentId}` | Принудительное удаление комментария | `204 No Content` |

---

## Тестирование

### Postman коллекции

| Файл                                      | Описание                                                        |
|-------------------------------------------|-----------------------------------------------------------------|
| `postman/ewm-main-service-spec.json`      | Тесты для Event Service, <br/>Request Service,<br/>User Service |
| `postman/ewm-stats-service-spec.json`     | Тесты для Stats Server                                          |
| `postman/comment-service.json` | Тесты для Comment Service                                       |

### Запуск тестов

1. Импортировать коллекции в Postman
2. Установить переменную `baseUrl = http://localhost:8080`
3. Запустить тесты

---

## Запуск

### 1. Запуск баз данных через Docker

```bash
docker-compose up -d
```

### 2. Запуск сервисов в правильном порядке

```bash
# 1. Eureka Server
cd eureka-server && mvn spring-boot:run

# 2. Config Server
cd config-server && mvn spring-boot:run

# 3. Stats Server
cd stats/stats-server && mvn spring-boot:run

# 4. User Service
cd user-service && mvn spring-boot:run

# 5. Request Service
cd request-service && mvn spring-boot:run

# 6. Event Service
cd event-service && mvn spring-boot:run

# 7. Comment Service
cd comment-service && mvn spring-boot:run

# 8. Gateway Server
cd gateway-server && mvn spring-boot:run
```

### 3. Проверка

```
# Eureka
http://localhost:8761

# Gateway Health
http://localhost:8080/actuator/health

# Swagger UI
http://localhost:8080/swagger-ui/index.html
```

---

## Технологии

### Основные

| Технология | Версия | Назначение |
|------------|--------|------------|
| Java | 21 | Язык программирования |
| Spring Boot | 3.2.1 | Основной фреймворк |
| Spring Cloud | 4.1.0 | Микросервисная инфраструктура |
| Spring Data JPA | 3.2.1 | Работа с базами данных |
| Spring Cloud Gateway | 4.1.0 | API Gateway |
| Eureka | 4.1.0 | Service Discovery |
| Feign | 4.1.0 | HTTP клиент |

### Дополнительные

| Технология | Назначение |
|------------|------------|
| PostgreSQL | База данных |
| Lombok | Упрощение кода |
| QueryDSL | Типобезопасные запросы |
| Resilience4j | Устойчивость к сбоям |
| Micrometer | Метрики и мониторинг |
| Docker | Контейнеризация |

---

## Структура проекта

```
graduate_project/
├── core/
│   ├── event-service/          # Управление событиями
│   ├── user-service/           # Управление пользователями
│   ├── request-service/        # Управление запросами
│   ├── comment-service/        # Управление комментариями
│   └── interaction-api/        # Общие DTO и клиенты
├── stats/
│   ├── stats-server/           # Сервис статистики
│   ├── stats-client/           # Клиент статистики
│   └── stats-dto/              # DTO статистики
├── infra/
│   ├── gateway-server/         # API Gateway
│   ├── eureka-server/          # Service Discovery
│   └── config-server/          # Config Server
├── docker-compose.yml          # Docker Compose
├── postman                     # Postman коллекции
└── README.md
```