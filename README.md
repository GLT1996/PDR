# PDR - 行人航位推算应用

一款基于手机传感器的室内定位应用，通过加速度计、陀螺仪、磁力计等传感器数据，实现无需GPS的行人轨迹追踪。

## 主要功能

### 1. 实时轨迹追踪
- 自动检测步伐
- 估计步长和行走方向
- 实时绘制行走轨迹
- 支持前进/后退方向检测

### 2. 轨迹可视化
- 2D轨迹平面图显示
- 支持缩放和平移手势
- 显示起点(红色)、终点(橙色)和行进方向箭头
- 实时更新步数、距离、时长统计

### 3. 历史轨迹管理
- 自动保存轨迹到本地数据库
- 查看历史轨迹列表
- 支持单条删除和批量删除
- 加载历史轨迹进行回放

### 4. 后台持续运行
- 前台服务支持息屏后继续记录
- 通知栏实时显示记录状态

## 技术方案

### 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  ┌─────────────────┐    ┌─────────────────────────┐     │
│  │  MainFragment   │───▶│  TrajectoryView         │     │
│  │  HistoryFragment│    │  (自定义轨迹绘制)        │     │
│  └─────────────────┘    └─────────────────────────┘     │
│           │                                              │
│           ▼                                              │
│  ┌─────────────────┐                                    │
│  │  MainViewModel  │                                    │
│  │  HistoryViewModel│                                   │
│  └─────────────────┘                                    │
└─────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────┐
│                   Domain Layer                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │              PDR Algorithm                        │    │
│  │  ┌───────────┐ ┌───────────┐ ┌─────────────────┐│    │
│  │  │StepDetect │ │StepEstim  │ │HeadingEstim    ││    │
│  │  │(步数检测)  │ │(步长估计)  │ │(航向估计)      ││    │
│  │  └───────────┘ └───────────┘ └─────────────────┘│    │
│  │  ┌─────────────────────────────────────────────┐│    │
│  │  │         PositionCalculator (位置计算)        ││    │
│  │  └─────────────────────────────────────────────┘│    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                            │
│  ┌─────────────────┐    ┌─────────────────────────┐     │
│  │ SensorController│    │   Room Database         │     │
│  │ (传感器管理)     │    │   TrajectoryDao         │     │
│  └─────────────────┘    └─────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────┐
│                    Service Layer                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │              PDRService (前台服务)                │    │
│  │  - 后台持续运行传感器监听                          │    │
│  │  - 通知栏显示记录状态                              │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### PDR算法原理

#### 1. 步数检测 (StepDetector)
- 使用加速度模量的峰值检测算法
- 滑动窗口平滑处理
- 动态阈值适应不同行走速度

#### 2. 步长估计 (StepLengthEstimator)
- 基于Weinberg模型：`步长 = K × √(加速度振幅)`
- 结合步频信息动态调整
- 默认步长约0.7米

#### 3. 航向估计 (HeadingEstimator)
- 融合陀螺仪角速度积分
- 磁力计提供绝对方向参考
- 互补滤波：`heading = 0.98 × gyro + 0.02 × mag`

#### 4. 前进/后退检测
- 分析步伐周期内前后加速度变化模式
- 前进：起步向前加速(正)，着地向后减速(负)
- 后退：起步向后加速(负)，着地向前减速(正)

### 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM |
| 异步 | Kotlin Coroutines + Flow |
| 数据库 | Room |
| 导航 | Navigation Component |
| UI绑定 | ViewBinding |
| 传感器 | Android SensorManager |

## 数据库结构

### 表结构

#### trajectories (轨迹表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 轨迹名称 |
| startTime | Long | 开始时间(毫秒时间戳) |
| endTime | Long | 结束时间(毫秒时间戳) |
| totalSteps | Int | 总步数 |
| totalDistance | Float | 总距离(米) |
| startPointName | String? | 起点名称(可选) |
| endPointName | String? | 终点名称(可选) |

#### trajectory_points (轨迹点表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| trajectoryId | Long | 外键，关联轨迹 |
| timestamp | Long | 时间戳 |
| x | Float | X坐标(米) |
| y | Float | Y坐标(米) |
| heading | Float | 航向角(弧度) |
| stepCount | Int | 累计步数 |

### ER图

```
┌─────────────────┐       ┌─────────────────────┐
│   trajectories  │       │  trajectory_points  │
├─────────────────┤       ├─────────────────────┤
│ id (PK)         │◄──────│ id (PK)             │
│ name            │       │ trajectoryId (FK)   │
│ startTime       │       │ timestamp           │
│ endTime         │       │ x                   │
│ totalSteps      │       │ y                   │
│ totalDistance   │       │ heading             │
│ startPointName  │       │ stepCount           │
│ endPointName    │       └─────────────────────┘
└─────────────────┘
        1 : N
```

## 项目结构

```
app/src/main/java/com/example/pdr/
├── App.kt                          # Application类
├── MainActivity.kt                 # 主Activity
├── data/
│   ├── local/
│   │   ├── TrajectoryDao.kt       # 数据库访问对象
│   │   └── TrajectoryDatabase.kt  # Room数据库
│   ├── model/
│   │   ├── Trajectory.kt          # 轨迹实体
│   │   ├── TrajectoryPoint.kt     # 轨迹点实体
│   │   └── SensorData.kt          # 传感器数据
│   └── repository/
│       ├── PDRRepository.kt       # PDR数据仓库
│       └── TrajectoryRepository.kt# 轨迹存储仓库
├── domain/
│   ├── algorithm/
│   │   ├── StepDetector.kt        # 步数检测
│   │   ├── StepLengthEstimator.kt # 步长估计
│   │   ├── HeadingEstimator.kt    # 航向估计
│   │   └── PositionCalculator.kt  # 位置计算
│   └── sensor/
│       └── SensorController.kt    # 传感器管理
├── service/
│   ├── PDRService.kt              # 前台服务
│   └── PDRServiceManager.kt       # 服务管理
└── ui/
    ├── main/
    │   ├── MainFragment.kt        # 主界面
    │   └── MainViewModel.kt       # 主界面ViewModel
    ├── history/
    │   ├── HistoryFragment.kt     # 历史记录界面
    │   ├── HistoryViewModel.kt    # 历史记录ViewModel
    │   └── TrajectoryAdapter.kt   # 列表适配器
    └── view/
        └── TrajectoryView.kt      # 轨迹绘制自定义View
```

## 权限说明

| 权限 | 用途 |
|------|------|
| ACTIVITY_RECOGNITION | 检测步伐(Android 10+) |
| FOREGROUND_SERVICE | 后台持续运行 |
| FOREGROUND_SERVICE_HEALTH | 健康类型前台服务(Android 14+) |
| POST_NOTIFICATIONS | 显示通知(Android 13+) |

## 注意事项

1. **传感器漂移**：长时间使用会累积误差，建议定期校准方向
2. **适用场景**：针对正常行走优化，跑步或快速移动可能不准确
3. **设备差异**：不同手机传感器精度不同，可能需要调整算法参数
4. **电量消耗**：持续使用传感器耗电较大

## 构建

```bash
# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

APK输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## License

MIT License