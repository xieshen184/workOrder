-- 医院后勤工单系统业务表结构
-- 适用：MySQL 5.7+/8.0，字符集 utf8mb4
-- 说明：沿用 RuoYi 的逻辑删除与审计字段，不建立物理外键，跨表一致性由服务层保证。

SET NAMES utf8mb4;

DROP TABLE IF EXISTS wo_notification_task;
DROP TABLE IF EXISTS wo_notification_template;
DROP TABLE IF EXISTS wo_sms_account_snapshot;
DROP TABLE IF EXISTS wo_evaluation;
DROP TABLE IF EXISTS wo_delay_request;
DROP TABLE IF EXISTS wo_process_record;
DROP TABLE IF EXISTS wo_action_log;
DROP TABLE IF EXISTS wo_assignment;
DROP TABLE IF EXISTS wo_attachment;
DROP TABLE IF EXISTS wo_engineer_status;
DROP TABLE IF EXISTS wo_order;
DROP TABLE IF EXISTS wo_sla_rule;
DROP TABLE IF EXISTS wo_category;

CREATE TABLE wo_category (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  parent_id             BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID',
  category_code         VARCHAR(32)  NOT NULL COMMENT '分类编码',
  category_name         VARCHAR(64)  NOT NULL COMMENT '分类名称',
  manager_dept_id       BIGINT       DEFAULT NULL COMMENT '默认负责部门ID',
  default_sla_rule_id   BIGINT       DEFAULT NULL COMMENT '默认SLA规则ID',
  order_num             INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status                CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  create_by             VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time           DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_by             VARCHAR(64)  DEFAULT '' COMMENT '更新者',
  update_time           DATETIME     DEFAULT NULL COMMENT '更新时间',
  remark                VARCHAR(500) DEFAULT NULL COMMENT '备注',
  del_flag              CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 2删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_category_code (category_code),
  KEY idx_wo_category_parent (parent_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单业务分类';

CREATE TABLE wo_sla_rule (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  rule_code             VARCHAR(32)  NOT NULL COMMENT '规则编码',
  rule_name             VARCHAR(64)  NOT NULL COMMENT '规则名称',
  category_id           BIGINT       DEFAULT NULL COMMENT '适用分类ID，空表示全局',
  urgency_level         TINYINT      DEFAULT NULL COMMENT '紧急程度：1一般 2紧急 3特急',
  response_minutes      INT          NOT NULL COMMENT '接单/响应时限（分钟）',
  arrival_minutes       INT          DEFAULT NULL COMMENT '到场时限（分钟）',
  finish_minutes        INT          DEFAULT NULL COMMENT '完工时限（分钟）',
  reminder_before_min   INT          NOT NULL DEFAULT 15 COMMENT '到期前提醒分钟数',
  allow_extension       CHAR(1)      NOT NULL DEFAULT '1' COMMENT '是否允许延期：0否 1是',
  status                CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
  create_by             VARCHAR(64)  DEFAULT '',
  create_time           DATETIME     DEFAULT NULL,
  update_by             VARCHAR(64)  DEFAULT '',
  update_time           DATETIME     DEFAULT NULL,
  remark                VARCHAR(500) DEFAULT NULL,
  del_flag              CHAR(1)      NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_sla_rule_code (rule_code),
  KEY idx_wo_sla_match (category_id, urgency_level, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单SLA规则';

CREATE TABLE wo_order (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  order_no              VARCHAR(32)   NOT NULL COMMENT '工单编号',
  title                 VARCHAR(128)  NOT NULL COMMENT '工单标题',
  category_id           BIGINT        NOT NULL COMMENT '分类ID',
  category_code         VARCHAR(32)   NOT NULL COMMENT '分类编码快照',
  category_name         VARCHAR(64)   NOT NULL COMMENT '分类名称快照',
  source_type           TINYINT       NOT NULL DEFAULT 1 COMMENT '来源：1手机端 2PC端 3接口导入',
  applicant_id          BIGINT        NOT NULL COMMENT '申请人用户ID',
  applicant_name        VARCHAR(64)   NOT NULL COMMENT '申请人姓名快照',
  applicant_phone       VARCHAR(32)   NOT NULL COMMENT '申请人联系电话',
  applicant_dept_id     BIGINT        DEFAULT NULL COMMENT '申请科室ID',
  applicant_dept_name   VARCHAR(64)   DEFAULT NULL COMMENT '申请科室名称快照',
  location              VARCHAR(255)  NOT NULL COMMENT '故障位置',
  urgency_level         TINYINT       NOT NULL COMMENT '紧急程度：1一般 2紧急 3特急',
  impact_scope          TINYINT       NOT NULL COMMENT '影响范围：1个人 2科室 3多科室 4全院',
  description           VARCHAR(1000) NOT NULL COMMENT '故障现象描述',
  possible_cause        VARCHAR(1000) DEFAULT NULL COMMENT '可能原因/补充说明',
  status                VARCHAR(32)   NOT NULL COMMENT '业务状态',
  current_assignee_id   BIGINT        DEFAULT NULL COMMENT '当前维修工程师ID',
  current_assignee_name VARCHAR(64)    DEFAULT NULL COMMENT '当前维修工程师姓名快照',
  sla_rule_id           BIGINT        DEFAULT NULL COMMENT '命中的SLA规则ID',
  response_deadline     DATETIME      DEFAULT NULL COMMENT '响应截止时间',
  arrival_deadline      DATETIME      DEFAULT NULL COMMENT '到场截止时间',
  finish_deadline       DATETIME      DEFAULT NULL COMMENT '完工截止时间',
  submitted_at          DATETIME      DEFAULT NULL COMMENT '提交时间',
  assigned_at           DATETIME      DEFAULT NULL COMMENT '最近分派时间',
  accepted_at           DATETIME      DEFAULT NULL COMMENT '接单时间',
  arrived_at            DATETIME      DEFAULT NULL COMMENT '到场时间',
  processing_at         DATETIME      DEFAULT NULL COMMENT '开始处理时间',
  finished_at           DATETIME      DEFAULT NULL COMMENT '工程师提交完工时间',
  confirmed_at          DATETIME      DEFAULT NULL COMMENT '申请人确认时间',
  closed_at             DATETIME      DEFAULT NULL COMMENT '关闭时间',
  response_overdue      CHAR(1)       NOT NULL DEFAULT '0' COMMENT '响应是否超期',
  arrival_overdue       CHAR(1)       NOT NULL DEFAULT '0' COMMENT '到场是否超期',
  finish_overdue        CHAR(1)       NOT NULL DEFAULT '0' COMMENT '完工是否超期',
  extension_deadline    DATETIME      DEFAULT NULL COMMENT '审批后的延期截止时间',
  version               INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  create_by             VARCHAR(64)   DEFAULT '',
  create_time           DATETIME      DEFAULT NULL,
  update_by             VARCHAR(64)   DEFAULT '',
  update_time           DATETIME      DEFAULT NULL,
  remark                VARCHAR(500)  DEFAULT NULL,
  del_flag              CHAR(1)       NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_order_no (order_no),
  KEY idx_wo_order_applicant (applicant_id, status, create_time),
  KEY idx_wo_order_assignee (current_assignee_id, status, create_time),
  KEY idx_wo_order_status (status, urgency_level, create_time),
  KEY idx_wo_order_response_deadline (status, response_overdue, response_deadline),
  KEY idx_wo_order_finish_deadline (status, finish_overdue, finish_deadline),
  KEY idx_wo_order_dept (applicant_dept_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';

CREATE TABLE wo_attachment (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  order_id              BIGINT        DEFAULT NULL COMMENT '工单ID，上传暂存时为空，提交后绑定',
  biz_stage             VARCHAR(32)   NOT NULL COMMENT '阶段：SUBMIT/ARRIVAL/PROCESS/FINISH/EVALUATION',
  biz_ref_id            BIGINT        DEFAULT NULL COMMENT '关联业务记录ID',
  file_name             VARCHAR(255)  NOT NULL COMMENT '原文件名',
  object_key            VARCHAR(512)  NOT NULL COMMENT '私有存储对象键，不作为公开URL返回',
  storage_provider      VARCHAR(32)   NOT NULL COMMENT '存储提供方：LOCAL/MINIO/OSS/COS',
  file_hash             VARCHAR(128)  DEFAULT NULL COMMENT '文件内容哈希',
  file_type             VARCHAR(128)  DEFAULT NULL COMMENT 'MIME类型',
  file_size             BIGINT        DEFAULT NULL COMMENT '文件大小（字节）',
  uploader_id           BIGINT        NOT NULL COMMENT '上传人ID',
  uploader_name         VARCHAR(64)   DEFAULT NULL COMMENT '上传人姓名',
  create_time           DATETIME      NOT NULL COMMENT '上传时间',
  del_flag              CHAR(1)       NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_attachment_object_key (object_key),
  KEY idx_wo_attachment_order (order_id, biz_stage, create_time),
  KEY idx_wo_attachment_biz_ref (biz_ref_id),
  KEY idx_wo_attachment_uploader (uploader_id, create_time),
  KEY idx_wo_attachment_hash (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件';

CREATE TABLE wo_assignment (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分派记录ID',
  order_id              BIGINT       NOT NULL COMMENT '工单ID',
  engineer_id           BIGINT       NOT NULL COMMENT '维修工程师ID',
  engineer_name         VARCHAR(64)  NOT NULL COMMENT '维修工程师姓名快照',
  engineer_dept_id      BIGINT       DEFAULT NULL COMMENT '维修部门ID',
  assigned_by           BIGINT       NOT NULL COMMENT '分派人ID',
  assigned_by_name      VARCHAR(64)  NOT NULL COMMENT '分派人姓名',
  assigned_at           DATETIME     NOT NULL COMMENT '分派时间',
  response_minutes      INT          NOT NULL COMMENT '本次响应时限',
  response_deadline     DATETIME     NOT NULL COMMENT '本次响应截止时间',
  assignment_status     VARCHAR(24)  NOT NULL COMMENT 'ACTIVE/ACCEPTED/REASSIGNED/RETURNED/CANCELLED',
  accepted_at           DATETIME     DEFAULT NULL COMMENT '接单时间',
  ended_at              DATETIME     DEFAULT NULL COMMENT '本次分派结束时间',
  return_reason         VARCHAR(500) DEFAULT NULL COMMENT '退回/转派原因',
  create_time           DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_wo_assignment_order (order_id, assigned_at),
  KEY idx_wo_assignment_engineer (engineer_id, assignment_status, assigned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单分派历史';

CREATE TABLE wo_action_log (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  order_id              BIGINT        NOT NULL COMMENT '工单ID',
  action_type           VARCHAR(32)   NOT NULL COMMENT '动作类型',
  from_status           VARCHAR(32)   DEFAULT NULL COMMENT '原状态',
  to_status             VARCHAR(32)   DEFAULT NULL COMMENT '目标状态',
  operator_id           BIGINT        NOT NULL COMMENT '操作人ID',
  operator_name         VARCHAR(64)   NOT NULL COMMENT '操作人姓名',
  operator_role         VARCHAR(32)   NOT NULL COMMENT '操作角色',
  action_content        VARCHAR(1000) DEFAULT NULL COMMENT '操作说明',
  ext_json              JSON          DEFAULT NULL COMMENT '扩展快照',
  idempotency_key       VARCHAR(64)   DEFAULT NULL COMMENT '幂等键',
  action_time           DATETIME      NOT NULL COMMENT '操作时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_action_idempotency (idempotency_key),
  KEY idx_wo_action_order (order_id, action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转审计日志';

CREATE TABLE wo_process_record (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '处理记录ID',
  order_id              BIGINT        NOT NULL COMMENT '工单ID',
  record_stage          VARCHAR(24)   NOT NULL COMMENT 'ARRIVAL/ASSESSMENT/PROGRESS/FINISH',
  engineer_id           BIGINT        NOT NULL COMMENT '工程师ID',
  engineer_name         VARCHAR(64)   NOT NULL COMMENT '工程师姓名',
  requires_parts        CHAR(1)       DEFAULT NULL COMMENT '是否需要零部件：0否 1是',
  parts_description     VARCHAR(500)  DEFAULT NULL COMMENT '零部件说明',
  assessed_hours        DECIMAL(8,2)  DEFAULT NULL COMMENT '评估工时',
  assessed_urgency      TINYINT       DEFAULT NULL COMMENT '现场评估紧急程度',
  assessed_scope        TINYINT       DEFAULT NULL COMMENT '现场评估影响范围',
  requires_extension    CHAR(1)       DEFAULT NULL COMMENT '是否需要延期',
  content               VARCHAR(2000) NOT NULL COMMENT '现场说明/处理过程/完工说明',
  occurred_at           DATETIME      NOT NULL COMMENT '业务发生时间',
  create_time           DATETIME      NOT NULL,
  PRIMARY KEY (id),
  KEY idx_wo_process_order (order_id, record_stage, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修处理记录';

CREATE TABLE wo_delay_request (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '延期申请ID',
  order_id              BIGINT        NOT NULL COMMENT '工单ID',
  applicant_id          BIGINT        NOT NULL COMMENT '申请工程师ID',
  original_deadline     DATETIME      NOT NULL COMMENT '原截止时间',
  requested_deadline    DATETIME      NOT NULL COMMENT '申请延期至',
  reason                VARCHAR(1000) NOT NULL COMMENT '延期原因',
  request_status        VARCHAR(16)   NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  approver_id           BIGINT        DEFAULT NULL COMMENT '审批人ID',
  approver_name         VARCHAR(64)   DEFAULT NULL COMMENT '审批人姓名',
  approval_comment      VARCHAR(500)  DEFAULT NULL COMMENT '审批意见',
  approved_at           DATETIME      DEFAULT NULL COMMENT '审批时间',
  create_time           DATETIME      NOT NULL,
  update_time           DATETIME      DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wo_delay_order (order_id, request_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单延期申请';

CREATE TABLE wo_evaluation (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  order_id              BIGINT       NOT NULL COMMENT '工单ID',
  evaluator_id          BIGINT       NOT NULL COMMENT '评价人ID',
  overall_score         TINYINT      NOT NULL COMMENT '总体评分1-5',
  response_score        TINYINT      DEFAULT NULL COMMENT '响应速度评分1-5',
  quality_score         TINYINT      DEFAULT NULL COMMENT '维修质量评分1-5',
  attitude_score        TINYINT      DEFAULT NULL COMMENT '服务态度评分1-5',
  evaluation_content    VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  evaluated_at          DATETIME     NOT NULL COMMENT '评价时间',
  create_time           DATETIME     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_evaluation_order (order_id),
  KEY idx_wo_evaluation_user (evaluator_id, evaluated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单服务评价';

CREATE TABLE wo_engineer_status (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '状态记录ID',
  engineer_id           BIGINT       NOT NULL COMMENT '工程师用户ID',
  engineer_name         VARCHAR(64)  NOT NULL COMMENT '工程师姓名快照',
  engineer_dept_id      BIGINT       DEFAULT NULL COMMENT '所属维修部门',
  duty_status           VARCHAR(16)  NOT NULL COMMENT 'OFF_DUTY/ON_DUTY',
  work_status           VARCHAR(16)  NOT NULL COMMENT 'AVAILABLE/WORKING/BUSY',
  current_order_id      BIGINT       DEFAULT NULL COMMENT '当前主工单ID',
  active_order_count    INT          NOT NULL DEFAULT 0 COMMENT '进行中工单数',
  overdue_order_count   INT          NOT NULL DEFAULT 0 COMMENT '超期工单数',
  status_source         VARCHAR(16)  NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL',
  last_changed_at       DATETIME     NOT NULL COMMENT '最近状态变化时间',
  update_time           DATETIME     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_engineer_status_user (engineer_id),
  KEY idx_wo_engineer_status (duty_status, work_status, overdue_order_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修工程师状态快照';

CREATE TABLE wo_notification_template (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  template_code         VARCHAR(64)   NOT NULL COMMENT '模板编码',
  event_type            VARCHAR(32)   NOT NULL COMMENT '触发事件',
  channel_type          VARCHAR(16)   NOT NULL COMMENT 'IN_APP/WECHAT/SMS',
  provider_template_id  VARCHAR(128)  DEFAULT NULL COMMENT '供应商模板ID',
  title_template        VARCHAR(255)  DEFAULT NULL COMMENT '标题模板',
  content_template      VARCHAR(2000) NOT NULL COMMENT '内容模板',
  status                CHAR(1)       NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
  create_by             VARCHAR(64)   DEFAULT '',
  create_time           DATETIME      DEFAULT NULL,
  update_by             VARCHAR(64)   DEFAULT '',
  update_time           DATETIME      DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_notify_template (template_code, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知模板';

CREATE TABLE wo_notification_task (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '通知任务ID',
  order_id              BIGINT        DEFAULT NULL COMMENT '关联工单ID',
  event_type            VARCHAR(32)   NOT NULL COMMENT '触发事件',
  channel_type          VARCHAR(16)   NOT NULL COMMENT 'IN_APP/WECHAT/SMS',
  receiver_user_id      BIGINT        DEFAULT NULL COMMENT '接收用户ID',
  receiver_address      VARCHAR(128)  NOT NULL COMMENT '手机号/OpenId等',
  template_code         VARCHAR(64)   DEFAULT NULL COMMENT '模板编码',
  title                 VARCHAR(255)  DEFAULT NULL COMMENT '消息标题',
  content               VARCHAR(2000) NOT NULL COMMENT '消息内容快照',
  task_status           VARCHAR(16)   NOT NULL COMMENT 'PENDING/SENDING/SUCCESS/FAILED/CANCELLED',
  retry_count           INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  max_retry_count       INT           NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  next_retry_at         DATETIME      DEFAULT NULL COMMENT '下次重试时间',
  provider_message_id   VARCHAR(128)  DEFAULT NULL COMMENT '供应商消息ID',
  error_message         VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  sent_at               DATETIME      DEFAULT NULL COMMENT '发送时间',
  create_time           DATETIME      NOT NULL,
  update_time           DATETIME      DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wo_notify_pending (task_status, next_retry_at),
  KEY idx_wo_notify_order (order_id, create_time),
  KEY idx_wo_notify_receiver (receiver_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知任务（事务外盒）';

CREATE TABLE wo_sms_account_snapshot (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  provider_code         VARCHAR(32)   NOT NULL COMMENT '短信供应商编码',
  account_name          VARCHAR(64)   DEFAULT NULL COMMENT '账号名称',
  balance_value         DECIMAL(16,4) DEFAULT NULL COMMENT '账户余额/剩余条数',
  balance_unit          VARCHAR(16)   DEFAULT NULL COMMENT '单位：CNY/COUNT',
  recharge_url          VARCHAR(500)  DEFAULT NULL COMMENT '充值跳转地址',
  query_status          VARCHAR(16)   NOT NULL COMMENT 'SUCCESS/FAILED',
  error_message         VARCHAR(500)  DEFAULT NULL COMMENT '查询失败原因',
  queried_at            DATETIME      NOT NULL COMMENT '查询时间',
  create_time           DATETIME      NOT NULL,
  PRIMARY KEY (id),
  KEY idx_wo_sms_snapshot (provider_code, queried_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信账户余额快照';

-- 推荐初始化数据。具体ID由部署脚本回填。
INSERT INTO wo_sla_rule
  (rule_code, rule_name, urgency_level, response_minutes, arrival_minutes, finish_minutes, reminder_before_min, allow_extension, status, create_by, create_time, del_flag)
VALUES
  ('SLA_15M', '15分钟响应', 3, 15, 15, 120, 5, '1', '0', 'admin', NOW(), '0'),
  ('SLA_2H',  '2小时响应',  2, 120, 120, 480, 30, '1', '0', 'admin', NOW(), '0'),
  ('SLA_6H',  '6小时响应',  1, 360, 360, 1440, 60, '1', '0', 'admin', NOW(), '0'),
  ('SLA_24H', '24小时响应', 1, 1440, 1440, 2880, 120, '1', '0', 'admin', NOW(), '0');

INSERT INTO wo_category
  (parent_id, category_code, category_name, order_num, status, create_by, create_time, del_flag)
VALUES
  (0, 'LOGISTICS', '后勤维护', 1, '0', 'admin', NOW(), '0'),
  (0, 'EQUIPMENT', '设备维护', 2, '0', 'admin', NOW(), '0'),
  (0, 'IT', '信息化维护', 3, '0', 'admin', NOW(), '0');
