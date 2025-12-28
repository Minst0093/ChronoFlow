# ChronoFlow 日历应用

## 一、产品概述

### 1.2 项目背景与定位

ChronoFlow 是一款基于 Kotlin Multiplatform 技术的跨平台日历应用，旨在以“时间流”为核心抽象重构日历展示与交互，提升时间密度感知与日程管理效率。产品定位与技术亮点如下：
- **核心价值**：以时间流视角重新组织日历信息，提供直观的时间密度感知与执行视图；  
- **技术特色**：Kotlin Multiplatform（shared domain 与引擎），Compose Multiplatform UI；  
- **设计原则**：领域驱动、接口抽象、跨端复用与可演进的扩展点。
---

## 二、产品功能介绍

### 2.1 核心功能模块

#### 2.1.1 日历视图模块

ChronoFlow 提供三种核心视图，满足不同使用场景的时间管理需求：

**月视图（时间密度视图）**
- 按月展示日期，通过颜色深度和节点大小表示当天事件数量与强度
- 支持左右滑动切换月份，提供流畅的浏览体验
- 点击日期可跳转到对应日期的日视图或周视图
- 展示农历信息和节日/节气，帮助用户把握传统时间节点

**周视图（结构视图）**
- 横轴表示星期（周一至周日），纵轴按小时或半小时切分
- 以块状方式展示事件区间，支持事件重叠的合理布局
- 支持滑动浏览上一周/下一周，提供连续的周际导航
- 清晰展示一周内的时间分配和事务分布

**日视图（精确执行视图）**
- 展示当天完整时间轴，提供24小时的连续时间线
- 显示当前时间指示线，实时更新（最小1分钟粒度）
- 高亮显示下一个即将发生的事件，提升执行效率
- 支持在时间轴空白区域长按创建新事件

#### 2.1.2 日程管理模块

**事件创建与编辑**
- 支持输入标题、设置时间区间（开始时间必须早于结束时间）
- 提供事件类型选择（学习/工作/生活/健康/娱乐）
- 设置事件强度等级（1-5级），用于月视图的热度可视化
- 配置提醒策略（支持提前5/10/30/60分钟等预设）
- 支持从任意视图触发创建入口

**事件查看与删除**
- 点击事件进入详情页，查看完整信息（标题、时间、类型、强度、备注、提醒设置）
- 支持编辑所有字段，修改后自动更新视图和提醒策略
- 提供删除功能，包含二次确认机制避免误删
- 删除后自动取消对应系统提醒并从所有视图移除

#### 2.1.3 智能提醒系统

- 支持单个事件配置0~多条提醒规则
- 提醒时间计算在共享层统一处理，确保跨端一致性
- 与各平台原生通知机制深度集成：
  - Android：使用 AlarmManager + NotificationCompat
  - iOS：使用 UNUserNotificationCenter
  - Desktop：使用系统通知框架
- 事件修改或删除时，提醒任务自动同步更新

### 2.2 扩展功能模块

#### 2.2.1 日历导入导出（RFC 5545 标准） — 接口保留，当前不实现

**说明**：项目保留了 `IcsService` 的接口和 Stub，实现 RFC 5545 的完整解析/导出是可行的工程项，但经产品决策本版本不再实现。若未来需要，可基于现有 Stub 快速补完解析与导出功能。

#### 2.2.2 网络订阅日历 — 接口保留，当前不实现

**说明**：`RemoteEventSource` 接口已存在且为 Stub；网络订阅功能（通过 URL 定期拉取 .ics）在本版本被放弃，保留为后续迭代的可选项。

#### 2.2.3 农历与节气支持 — 已实现（本地离线）

- 在月/周/日视图中显示农历短文本与节气信息；事件详情显示完整农历信息；实现基于本地查表与近似节气算法的 `DefaultLunarCalendarService`，覆盖 1900–2050 年常见日期范围。  
- 该实现离线、低延迟并内置缓存策略（默认内存缓存），满足日常使用场景。

### 2.3 核心数据模型

```kotlin
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val type: EventType,
    val intensity: Int, // 1-5
    val reminder: ReminderConfig?,
    val recurrence: RecurrenceRule?, // 重复规则
    val allDay: Boolean = false // 全天事件
)

enum class EventType {
    STUDY, WORK, LIFE, HEALTH, ENTERTAINMENT
}
```

### 2.4 用户使用场景

1. **学生用户**：管理课程表、作业deadline、考试安排，通过月视图把握学习节奏
2. **职场用户**：跟踪会议安排、项目节点、个人任务，通过周视图规划工作周
3. **日常生活**：记录健身计划、家庭事务、娱乐活动，通过日视图把握当天安排
4. **技术学习者**：实践跨端开发技术，学习 RFC 5545 协议实现
5. **传统节日规划**：结合农历信息，合理安排节日和节气相关的活动

---

## 三、程序概要设计

### 3.1 技术方案选择

#### 3.1.1 核心技术栈

- **开发框架**：Kotlin Multiplatform (KMP)
- **UI框架**：Compose Multiplatform（Android/iOS/Desktop）
- **数据库**：SQLDelight（跨平台数据库解决方案）
- **依赖注入**：Koin（轻量级跨平台依赖注入）
- **时间处理**：kotlinx-datetime（跨平台时间日期库）
- **序列化**：kotlinx.serialization（跨平台数据序列化）

#### 3.1.2 平台支持

- **Android**：原生 Android 应用，支持 API 21+
- **iOS**：原生 iOS 应用，支持 iOS 13+
- **Desktop**：跨平台桌面应用（Windows/macOS/Linux）
- **鸿蒙**：预留适配点，未来可通过 Compose 桥接实现

### 3.2 架构设计原则

#### 3.2.1 分层架构设计

ChronoFlow 采用五层架构设计，确保职责分离和代码复用：

1. **UI层**：Compose Multiplatform 实现，负责界面渲染和用户交互
2. **Presentation层**：ViewModel 和状态管理，处理UI逻辑和数据流
3. **Domain层**：核心业务逻辑，包含事件模型、时间聚合、提醒计算等
4. **Data层**：数据访问层，负责本地存储和外部数据源管理
5. **Platform层**：平台适配层，封装各平台的系统能力

#### 3.2.2 设计原则

- **单一职责**：每个模块职责明确，避免功能耦合
- **依赖倒置**：高层模块不依赖低层实现，通过接口解耦
- **跨端复用**：核心业务逻辑在shared模块实现，最大化代码复用
- **可扩展性**：为云同步、多日历、统计分析等功能预留扩展接口

### 3.3 关键模块设计

#### 3.3.1 Domain层核心引擎

**TimeAggregationEngine**
```kotlin
interface TimeAggregationEngine {
    fun aggregateByDay(events: List<CalendarEvent>): List<DaySummary>
    fun aggregateByWeek(events: List<CalendarEvent>): List<WeekSummary>
}
```
- 负责将原始事件列表聚合为视图所需的数据结构
- 为月视图提供日汇总，为周视图提供周汇总
- 支持跨天事件和重复事件的正确处理

**ReminderEngine**
```kotlin
interface ReminderEngine {
    fun calculateReminderTimes(event: CalendarEvent): List<LocalDateTime>
}
```
- 根据事件和提醒配置计算实际触发时间
- 支持多提醒点和重复事件提醒计算
- 确保跨端提醒逻辑的一致性

#### 3.3.2 Data层设计

**EventRepository**
```kotlin
interface EventRepository {
    suspend fun getEvents(start: LocalDate, end: LocalDate): List<CalendarEvent>
    suspend fun saveEvent(event: CalendarEvent)
    suspend fun deleteEvent(eventId: String)
}
```
- 定义数据访问接口，支持CRUD操作
- 采用挂起函数支持异步操作
- 通过接口抽象屏蔽存储实现细节

**数据持久化方案**
- 本地存储：SQLDelight 实现跨平台数据库访问
- 外部数据：ICS 解析和网络订阅支持
- 数据同步：预留云同步扩展接口

#### 3.3.3 Platform适配层

**NotificationScheduler**
```kotlin
interface NotificationScheduler {
    fun schedule(event: CalendarEvent)
    fun cancel(eventId: String)
}
```
- 封装各平台的通知调度能力
- Android：AlarmManager + Notification
- iOS：UNUserNotificationCenter
- Desktop：系统级通知框架

### 3.4 数据流设计

#### 3.4.1 单向数据流

```
User Action → View (Compose UI) → ViewModel → Domain Logic → Data Layer → Platform
      ↑                                                                       ↓
      └─────────────────────── UI State Update ←──────────────────────────────┘
```

- 用户操作触发ViewModel意图
- ViewModel调用Domain层业务逻辑
- Domain层通过Repository访问数据
- 状态变更通过StateFlow通知UI更新
- 确保数据流向的清晰和可预测性

#### 3.4.2 状态管理

- 使用StateFlow管理UI状态
- ViewModel作为状态容器和业务逻辑协调者
- 通过组合状态提供视图所需的数据
- 支持状态的持久化和恢复

### 3.5 关键技术实现

#### 3.5.1 时间聚合算法

- 支持跨天事件的正确分割和展示
- 处理事件重叠的布局计算
- 优化大数据集的聚合性能
- 缓存机制减少重复计算

#### 3.5.2 重复事件处理

- 基于 RecurrenceRule 模型支持重复事件
- 提供 RecurrenceExpander 进行事件实例化
- 支持多种重复模式（每日/每周/每月/每年）
- 灵活的结束条件（次数限制/日期限制）

---

## 四、技术亮点与实现原理

### 4.1 跨端领域驱动实现（KMP）
- 核心业务逻辑（事件模型、重复展开、提醒计算、农历服务）统一放在 shared 模块，保证多端行为一致并减少重复实现工作量。

### 4.2 重复事件的按需展开（RecurrenceExpander）
- 设计思想：不在存储层物化所有重复实例，而是基于 `RecurrenceRule` 在视窗级别按需展开 occurrences。  
- 优点：避免无限重复实例化、降低存储与计算压力；通过缓存常见视窗（按月/周）提升响应速度。  
- 实现要点：支持 `freq/interval/byDay/count/until`，并在视窗内生成 occurrence 的 LocalDateTime 列表供视图与提醒使用。

### 4.3 统一提醒引擎与平台调度隔离
- `ReminderEngine` 负责跨端一致的提醒时间计算（包括重复事件的多次提醒点）；平台层仅负责调度与触发（`NotificationScheduler`）。  
- Android 实现通过为每个 occurrence 注册 Alarm（基于 eventId+index 生成 requestCode），并在事件修改/删除时批量取消，保证提醒一致性。

### 4.4 本地农历/节气实现（离线、低延迟）
- 采用本地查表 + 近似节气计算，覆盖 1900–2050 年常见节日与节气；组合内存缓存（LRU-like）降低重复计算开销。  
- 提供同步查询接口 `getLunarInfo(date)`，便于 UI 在刷新月视图时批量填充 DaySummary。

### 4.5 高保真 UI 与交互优化（Compose）
- 日视图时间轴采用精确时间块渲染、跨小时事件连续显示；周视图改进并排重叠布局与过渡动画；月视图支持弹出底部事件列表。  
- 表单实现实时校验、重复规则即时预览与保存反馈，显著提升可用性。

---

## 四、软件架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Compose Multiplatform)         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Android App    │  iOS App    │  Desktop App  │ 鸿蒙 │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────▲───────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────┐
│    Presentation Layer (ViewModel & UI State)                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  CalendarViewModel │  CalendarUiState  │  StateFlow │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────▲───────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────┐
│        Domain Layer (Business Logic)                        │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ CalendarEvent│  TimeAggregationEngine │ ReminderEngine │ │
│  │ EventType    │  LunarCalendarService   │  EventUtils   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────▲───────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────┐
│        Data Layer (Data Access)                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ EventRepository │ SQLDelight │ ICS Service │ Remote │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────▲───────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────┐
│    Platform Adapter Layer (System Integration)              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Notification   │  File System  │  Network  │  Storage │ │
│  │  Scheduler      │  Access       │  Access   │  Access  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

模块依赖关系：
• UI Layer → Presentation Layer → Domain Layer → Data Layer → Platform Layer
• Shared Module 包含：Domain + Data + Presentation + Platform Interfaces
• 各平台 App 主要负责 UI 实现和 Platform Adapter 实现
```

### 4.1 架构特点

1. **清晰的分层**：五层架构确保职责分离，每层关注特定领域
2. **跨端复用**：核心业务逻辑在shared模块实现，最大化代码复用
3. **接口抽象**：通过接口定义模块边界，支持灵活的实现替换
4. **依赖方向**：严格遵循依赖倒置，上层依赖下层抽象
5. **扩展性**：为未来功能扩展预留清晰的接口和扩展点

### 4.2 关键设计模式

- **Repository模式**：数据访问抽象，屏蔽存储细节
- **Engine模式**：业务逻辑封装，提供可复用的计算服务
- **Adapter模式**：平台能力适配，统一跨端接口
- **Observer模式**：状态管理，通过StateFlow实现响应式更新

---

## 五、项目实施计划

| 阶段 | 主要内容 | 对应功能 |
|------|----------|----------|
| 第一阶段 | 领域模型冻结与基础架构搭建 | Domain v0.9 模型定义 |
| 第二阶段 | 核心视图实现（月/周/日视图） | 基本功能：日历视图展示 |
| 第三阶段 | 日程管理功能（增删改查） | 基本功能：日程管理 |
| 第四阶段 | 提醒系统与平台集成 | 基本功能：提醒功能 |
| 第五阶段 | 扩展功能实现（导入导出、订阅、农历） | 扩展功能 |
| 第六阶段 | 性能优化与测试完善 | 产品质量提升 |

---

## 六、总结

### 6.1 项目成果

- **技术创新**：成功应用 KMP 技术实现跨端复用，验证了多平台开发的可行性。  
- **架构设计**：建立了清晰的分层架构，为后续扩展奠定了坚实基础。  
- **功能覆盖**：实现了完整的日历管理核心功能（视图、日程管理、重复事件、提醒、本地农历）；网络订阅与 ICS 导出暂未实现，但接口保留以便未来扩展。  
- **离线能力**：本地农历/节气服务保证离线可用，满足无网场景下的时间信息展示。

### 6.2 未来扩展方向

1. **云同步功能**：实现多设备数据同步和备份
2. **智能推荐**：基于用户习惯提供日程安排建议
3. **统计分析**：提供时间使用统计和效率分析
4. **多日历管理**：支持多个日历的统一管理和切换
5. **鸿蒙生态**：完成鸿蒙平台的适配和发布
