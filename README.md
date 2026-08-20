# AI 经营材料智能处理平台（MVP）

面向经营汇报材料的标准化处理流水线：选择分析主题 → 材料上传 → 材料解析 Agent → AI 章节归并事实源 → 材料预审 → 模型映射 → 经营分析 → 知识提取 → 轻量汇总。前端按“经营材料 → 经营雷达 → 经营追问 → 事项跟踪”组织业务使用，按“主题管理 → 规则包管理 → 模型管理”组织配置中心。

## 技术栈

- 后端：Java 21 + Spring Boot 3.5.15 + MyBatis + Flyway + Apache POI + PDFBox
- 前端：Vue 3 + TypeScript + Vite + Pinia + Element Plus
- 数据库：PostgreSQL 16（`fin_agent` 库，Flyway 自动建表）
- 任务：Spring @Async + 内存 `TaskStateStore` + SSE 进度推送
- AI：Anthropic Compatible / OpenAI Compatible 接口（模型配置管理），默认接入火山方舟 `doubao-seed-2.1-turbo`

## 目录结构

```text
fin_agent/
├── backend/     Spring Boot 后端（com.huawei.fin.ai.material 业务模块分包）
├── frontend/    Vue 3 前端（工作台/材料中心/智能预审/经营洞察/系统设置）
├── docs/        方案文档
└── scripts/     启动脚本
```

后端按业务模块分包：`material / task / validation / knowledge / summary / analysis`，跨模块编排在 `agent/MaterialAgentOrchestrator`，公共能力在 `common`。

前端采用产品级信息架构，公共组件按 `components/common`、`components/material`、`components/validation`、`components/knowledge`、`components/ai` 组织；单材料分析使用三栏工作台（处理进度 / 当前任务 / 证据详情），底层 Agent 日志默认隐藏。

## 环境

环境目录：`D:\A_app`

- JDK 21：`D:\A_app\jdk-21.0.12+8`
- Maven 3.9：`D:\A_app\apache-maven-3.9.9`
- Node 18：`D:\A_app\node-v18.20.4-win-x64`
- PostgreSQL：`D:\A_app\pgsql`

## 启动

1. 启动 PostgreSQL（首次需初始化，已初始化过则直接启动）：

```powershell
& 'D:\A_app\pgsql\bin\pg_ctl.exe' -D 'D:\A_app\pgsql\data' -l 'D:\A_app\pgsql\pg.log' start
```

数据库连接默认：`jdbc:postgresql://localhost:5432/fin_agent`，用户 `postgres`，密码 `postgres`。

2. 构建并启动后端：

```powershell
cd D:\A_code\AI\fin_agent\backend
$env:JAVA_HOME='D:\A_app\jdk-21.0.12+8'
$env:Path="$env:JAVA_HOME\bin;D:\A_app\apache-maven-3.9.9\bin;$env:Path"
mvn -DskipTests package
java -jar target\ai-material-backend-0.1.0-SNAPSHOT.jar
```

后端地址：http://localhost:60001

3. 启动前端：

```powershell
cd D:\A_code\AI\fin_agent\frontend
$env:Path="D:\A_app\node-v18.20.4-win-x64;$env:Path"
npm install
npm run dev
```

前端地址：http://localhost:5173

## 快速验证

1. 打开 http://localhost:5173
2. 在“经营材料 → 新建分析”上传 `backend\data\samples\sample_beijing_2026q2.pptx`，填写地区部、代表处、期间并选择分析主题
3. 上传后自动进入材料详情：顶部实时展示“AI 正在处理”，完成后依次呈现 AI 预审结论、模型数据、AI 经营摘要、经营洞察
4. 在“AI 预审”页可对已处理材料执行材料预审、模型映射、经营分析，查看预审问题、模型字段和结构化结论
5. 在“系统设置 → 主题管理”管理主题名称、描述、模型字段、规则包；在“预审规则”管理规则条目；在“模型配置”管理/测试模型
6. 材料详情的“AI 处理过程”或“查看 AI 执行详情”可查看 Agent / Skill / Prompt / Response / 耗时 / Token

样例材料内置了可被识别的问题：收入跨页不一致、同比复算不一致、现金流为负、库存环比异常、页数偏少。

## 模型配置

默认模型配置（火山方舟，OpenAI Compatible 协议）：

```text
Provider:   OPENAI_COMPATIBLE
Base URL:   https://ark.cn-beijing.volces.com/api/plan/v3
Model:      doubao-seed-2.1-turbo
Headers:    Authorization: Bearer <api-key>
```

可在“模型配置”页新增/编辑模型、启停、测试连通。当前实现关闭 thinking 模式以加快推理（单次调用约 3~10 秒），超时默认 300 秒并自动重试一次。

大模型参与环节：

- ChapterSplitter：根据标题归并章节，保留 slideRange，无法识别时按页码拆分
- PreAuditAgent：基于事实源与预审核规则包生成材料预审结论
- FactMappingAgent：将事实源映射为模型数据
- BusinessAnalysisAgent：基于模型数据与专家经验规则生成经营分析结论
- DataValidationAgent：对规则引擎发现的问题生成复核与整改建议
- KnowledgeExtractionAgent：提取经营亮点、风险、事项
- ConsolidationAgent：生成经营摘要

每次调用都会记录到 `ai_llm_trace`，可在处理过程窗口或材料详情的“处理过程”页查看。

处理结果（指标、知识、问题、AI 摘要、经营评分、LLM 痕迹）在流水线完成后全部落库；材料详情直接读取数据库展示，不重复调用大模型。

## 前端页面

- 经营材料：按“地区部 → 期间 → 代表处 → 材料”层级归集，支持新建分析
- 单材料工作台：默认展示“经营简报”分析结果页（经营概览、核心指标、五大经营分析、AI 重点发现、重点事项、数据与证据），另设“处理详情”页签查看处理进度与 AI 执行记录
- 经营雷达：基于各代表处经营简报横向汇总，展示核心指标卡片、风险等级与 AI 共性问题
- 经营追问：范围化经营数据问答
- 事项跟踪：AI 分析发现自动沉淀为事项，支持待跟进/跟进中/已闭环
- 配置中心：主题管理（名称/描述/模型/规则包）、规则包与规则条目、模型管理
- 处理记录：`/tasks` 保留，从工作台“最近处理材料”和材料中心“处理记录”进入

## 主要 API

```text
POST   /api/materials/upload        上传并创建处理任务
GET    /api/materials               材料列表
GET    /api/materials/{id}          材料详情
GET    /api/materials/{id}/slides   页面解析结果
GET    /api/materials/{id}/ir       Business IR
GET    /api/materials/{id}/metrics  经营指标
GET    /api/materials/{id}/knowledge 知识提取结果
GET    /api/materials/{id}/summary  经营汇总
GET    /api/materials/{id}/briefing 经营简报（经营概览/核心指标/五维分析/发现/事项/证据）
GET    /api/materials/{id}/traces   LLM 调用痕迹
GET    /api/tasks/{id}/traces       LLM 调用痕迹
POST   /api/materials/{id}/retry    重新处理
GET    /api/themes                  分析主题（含模型字段与规则包）
POST   /api/themes                  新建主题（含模型与规则包）
PUT    /api/themes/{id}             更新主题
DELETE /api/themes/{id}             删除主题
GET/POST /api/follow-ups            事项跟踪列表/新建
POST   /api/follow-ups/sync         从经营分析自动沉淀事项
PUT    /api/follow-ups/{id}/status  更新事项状态
POST   /api/materials/{id}/fact-sources/generate  AI 章节归并生成事实源
GET    /api/materials/{id}/fact-sources           事实源列表
POST   /api/materials/{id}/pre-audit              材料预审（themeId）
GET    /api/materials/{id}/pre-audit              预审结果
POST   /api/materials/{id}/model-data/map         模型映射（themeId）
GET    /api/materials/{id}/model-data             模型数据
POST   /api/materials/{id}/analysis               经营分析（themeId）
GET    /api/materials/{id}/analysis               分析结论
GET/POST/PUT/DELETE /api/rule-packages            规则包管理
GET/POST/PUT/DELETE /api/rule-packages/{id}/items 规则条目管理
GET    /api/tasks                   任务列表
GET    /api/tasks/{id}              任务详情与进度
GET    /api/tasks/{id}/events       SSE 进度流
POST   /api/tasks/{id}/cancel       取消任务
GET    /api/validations             校验问题清单（支持 materialId/category/status/severity 过滤）
POST   /api/validations/{id}/confirm 人工确认
POST   /api/validations/{id}/ignore  标记误报
GET/POST/PUT/DELETE /api/rules      预审规则库管理
GET/POST/PUT/DELETE /api/models     模型配置管理
POST   /api/models/{id}/test        模型连通测试
```

## 本期边界

- 未接入 OCR、视觉模型和页面渲染，解析以文本/表格为主
- Redis 未启用，任务进度使用内存 `TaskStateStore`
- 第一版预置 2 个分析主题：经营情况（14 个模型字段、2 个规则包）与代表处经营分析（32 个模型字段、3 个规则包，含预审/质量/风险）
- 经营分析会同时聚合主题下的专家经验规则包与风险分析规则包，由 AI 结合模型数据给出经营结论
- 预置北京、上海两份代表处经营简报演示数据，可直接在经营雷达和经营简报页查看完整效果
- 模型映射失败会中断任务并明确报错，不再静默标记完成
