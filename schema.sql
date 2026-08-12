-- guardrail DB schema
-- source: SHOW CREATE TABLE on live RDS (database-1.c1eg6m840npc.ap-southeast-2.rds.amazonaws.com/guardrail)
-- generated 2026-08-12, ordered for FK-safe creation

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `test_data`;
DROP TABLE IF EXISTS `USER_REPO`;
DROP TABLE IF EXISTS `REPO_FAV`;
DROP TABLE IF EXISTS `REPO_EMBEDDING`;
DROP TABLE IF EXISTS `REFRESH_TOKEN`;
DROP TABLE IF EXISTS `PULL_REQUEST_ANALYSIS`;
DROP TABLE IF EXISTS `NOTIFICATION`;
DROP TABLE IF EXISTS `GITHUB_PULL_REQUEST`;
DROP TABLE IF EXISTS `FINDING`;
DROP TABLE IF EXISTS `ANNOUNCEMENT_FILE`;
DROP TABLE IF EXISTS `ANNOUNCEMENT`;
DROP TABLE IF EXISTS `ANALYSIS`;
DROP TABLE IF EXISTS `USER`;
DROP TABLE IF EXISTS `GITHUB_REPO`;
DROP TABLE IF EXISTS `COMPANY`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `COMPANY` (
  `company_id` int NOT NULL AUTO_INCREMENT COMMENT '기업 고유 번호',
  `name` varchar(255) NOT NULL COMMENT '기업명',
  `created_at` date NOT NULL COMMENT '등록일',
  `updated_at` date NOT NULL COMMENT '수정일',
  PRIMARY KEY (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `GITHUB_REPO` (
  `repo_id` int NOT NULL AUTO_INCREMENT COMMENT '레포 고유 번호',
  `name` varchar(255) NOT NULL COMMENT '이름',
  `repo_url` varchar(255) NOT NULL COMMENT '레포 url',
  `created_at` date NOT NULL COMMENT '연동일',
  `language` varchar(255) DEFAULT NULL,
  `last_updated` varchar(255) DEFAULT NULL,
  `is_private` tinyint(1) DEFAULT '0',
  `organization` varchar(255) NOT NULL,
  `webhook_id` bigint DEFAULT NULL,
  `webhook_active` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`repo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `USER` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '유저 고유 번호',
  `company_id` int DEFAULT NULL COMMENT '기업 고유 번호',
  `name` varchar(255) NOT NULL COMMENT '이름',
  `login_id` varchar(255) NOT NULL COMMENT '아이디',
  `password` varchar(255) NOT NULL COMMENT '비밀번호',
  `role` varchar(255) NOT NULL COMMENT '역할',
  `git_id` varchar(255) DEFAULT NULL COMMENT 'GIT ID',
  `git_name` varchar(255) DEFAULT NULL COMMENT 'GIT 계정명',
  `created_at` date NOT NULL COMMENT '가입일',
  `updated_at` date NOT NULL COMMENT '수정일',
  `github_access_token` varchar(255) DEFAULT NULL COMMENT 'GitHub OAuth 액세스 토큰 (암호화 저장)',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `login_id` (`login_id`),
  KEY `company_id` (`company_id`),
  CONSTRAINT `USER_ibfk_1` FOREIGN KEY (`company_id`) REFERENCES `COMPANY` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ANALYSIS` (
  `analysis_id` int NOT NULL AUTO_INCREMENT COMMENT '분석 고유 번호',
  `repo_id` int NOT NULL COMMENT '레포 고유 번호',
  `company_id` int NOT NULL COMMENT '기업 고유 번호',
  `user_id` int NOT NULL COMMENT '유저 고유 번호',
  `origin_code` text NOT NULL COMMENT '원본 코드',
  `language` varchar(255) NOT NULL COMMENT '언어',
  `file_path` varchar(500) DEFAULT NULL,
  `prompt` text,
  `status` varchar(255) NOT NULL COMMENT '상태',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `branch` varchar(255) DEFAULT NULL,
  `source_blob_sha` varchar(64) DEFAULT NULL COMMENT '분석 시점 GitHub 파일 Blob SHA',
  `pushed_commit_sha` varchar(64) DEFAULT NULL COMMENT 'GitHub Push 성공 Commit SHA',
  `pushed_at` datetime DEFAULT NULL COMMENT 'GitHub Push 성공 시간',
  `improvable_ratio` decimal(5,1) DEFAULT NULL COMMENT '이슈가 걸친 고유 줄 수 / 전체 줄 수 * 100',
  PRIMARY KEY (`analysis_id`),
  KEY `repo_id` (`repo_id`),
  KEY `company_id` (`company_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `ANALYSIS_ibfk_1` FOREIGN KEY (`repo_id`) REFERENCES `GITHUB_REPO` (`repo_id`),
  CONSTRAINT `ANALYSIS_ibfk_2` FOREIGN KEY (`company_id`) REFERENCES `COMPANY` (`company_id`),
  CONSTRAINT `ANALYSIS_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ANNOUNCEMENT` (
  `board_id` int NOT NULL AUTO_INCREMENT COMMENT '글 고유 번호',
  `user_id` int NOT NULL COMMENT '유저 고유 번호',
  `title` varchar(255) NOT NULL COMMENT '제목',
  `content` text NOT NULL COMMENT '내용',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '조회수',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '고정 여부',
  PRIMARY KEY (`board_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `ANNOUNCEMENT_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ANNOUNCEMENT_FILE` (
  `file_id` int NOT NULL AUTO_INCREMENT,
  `board_id` int NOT NULL,
  `original_file_name` varchar(255) NOT NULL,
  `stored_file_name` varchar(255) NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `file_size` bigint NOT NULL,
  PRIMARY KEY (`file_id`),
  KEY `fk_announcement_file_board` (`board_id`),
  CONSTRAINT `fk_announcement_file_board` FOREIGN KEY (`board_id`) REFERENCES `ANNOUNCEMENT` (`board_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `FINDING` (
  `result_id` int NOT NULL AUTO_INCREMENT COMMENT '고유 번호',
  `analysis_id` int NOT NULL COMMENT '분석 고유 번호',
  `inefficiency_result` text COMMENT '비효율 코드 분석 결과',
  `modified_code` text COMMENT '수정된 코드',
  `secu_result` text COMMENT '보안 이슈 결과',
  `duplicate_result` text COMMENT '중복 코드 탐지 결과',
  `created_at` date NOT NULL COMMENT '생성일',
  `is_ai_generated` tinyint(1) NOT NULL COMMENT 'AI 코드 여부',
  `total_issues` int NOT NULL DEFAULT '0' COMMENT '이슈 수',
  `security_count` int NOT NULL DEFAULT '0' COMMENT '보안 취약점 건수',
  `inefficiency_count` int NOT NULL DEFAULT '0' COMMENT '비효율 건수',
  `ai_probability` double DEFAULT NULL,
  PRIMARY KEY (`result_id`),
  KEY `analysis_id` (`analysis_id`),
  CONSTRAINT `FINDING_ibfk_1` FOREIGN KEY (`analysis_id`) REFERENCES `ANALYSIS` (`analysis_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `GITHUB_PULL_REQUEST` (
  `github_pull_request_id` int NOT NULL AUTO_INCREMENT COMMENT 'PR 내부 고유 번호',
  `user_id` int NOT NULL COMMENT 'PR 생성 사용자',
  `repo_id` int NOT NULL COMMENT 'GitHub 저장소',
  `github_pr_number` int NOT NULL COMMENT 'GitHub PR 번호',
  `title` varchar(255) NOT NULL COMMENT 'PR 제목',
  `description` text COMMENT 'PR 설명',
  `head_branch` varchar(255) NOT NULL COMMENT '변경사항이 포함된 브랜치',
  `base_branch` varchar(255) NOT NULL COMMENT '병합 대상 브랜치',
  `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, MERGED, CLOSED',
  `is_draft` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Draft PR 여부',
  `pr_url` varchar(500) NOT NULL COMMENT 'GitHub PR URL',
  `head_commit_sha` varchar(64) DEFAULT NULL COMMENT 'PR 생성 시점 Head Commit SHA',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'PR 생성 시간',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'PR 수정 시간',
  `merged_at` datetime DEFAULT NULL COMMENT 'PR 병합 시간',
  PRIMARY KEY (`github_pull_request_id`),
  UNIQUE KEY `uk_gpr_repo_number` (`repo_id`,`github_pr_number`),
  KEY `idx_gpr_user_created` (`user_id`,`created_at`),
  KEY `idx_gpr_repo_status` (`repo_id`,`status`),
  CONSTRAINT `fk_gpr_repo` FOREIGN KEY (`repo_id`) REFERENCES `GITHUB_REPO` (`repo_id`),
  CONSTRAINT `fk_gpr_user` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `chk_gpr_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'MERGED',_utf8mb4'CLOSED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `NOTIFICATION` (
  `notification_id` int NOT NULL AUTO_INCREMENT COMMENT '알림 고유 번호',
  `user_id` int NOT NULL COMMENT '유저 고유 번호',
  `analysis_id` int DEFAULT NULL COMMENT '분석 고유 번호',
  `board_id` int DEFAULT NULL,
  `type` varchar(255) NOT NULL COMMENT '종류',
  `message` varchar(255) NOT NULL COMMENT '내용',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '읽음 여부',
  `created_at` datetime NOT NULL,
  `title` varchar(255) NOT NULL COMMENT '알림제목',
  PRIMARY KEY (`notification_id`),
  KEY `user_id` (`user_id`),
  KEY `NOTIFICATION_ibfk_2` (`analysis_id`),
  KEY `fk_notification_announcement` (`board_id`),
  CONSTRAINT `fk_notification_announcement` FOREIGN KEY (`board_id`) REFERENCES `ANNOUNCEMENT` (`board_id`),
  CONSTRAINT `NOTIFICATION_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`),
  CONSTRAINT `NOTIFICATION_ibfk_2` FOREIGN KEY (`analysis_id`) REFERENCES `ANALYSIS` (`analysis_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `PULL_REQUEST_ANALYSIS` (
  `pull_request_analysis_id` int NOT NULL AUTO_INCREMENT COMMENT 'PR-분석 연결 고유 번호',
  `github_pull_request_id` int NOT NULL COMMENT 'PR 내부 고유 번호',
  `analysis_id` int NOT NULL COMMENT '분석 고유 번호',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '연결 생성 시간',
  PRIMARY KEY (`pull_request_analysis_id`),
  UNIQUE KEY `uk_pra_pr_analysis` (`github_pull_request_id`,`analysis_id`),
  KEY `idx_pra_analysis` (`analysis_id`),
  CONSTRAINT `fk_pra_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `ANALYSIS` (`analysis_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pra_pull_request` FOREIGN KEY (`github_pull_request_id`) REFERENCES `GITHUB_PULL_REQUEST` (`github_pull_request_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `REFRESH_TOKEN` (
  `refresh_token_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`refresh_token_id`),
  UNIQUE KEY `uk_refresh_token_user` (`user_id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `REPO_EMBEDDING` (
  `id` int NOT NULL AUTO_INCREMENT,
  `repo_id` int DEFAULT NULL,
  `file_path` varchar(1000) NOT NULL,
  `start_line` int DEFAULT NULL,
  `end_line` int DEFAULT NULL,
  `faiss_vector_id` int NOT NULL,
  `function_name` varchar(255) DEFAULT NULL,
  `parameters` text,
  `code_snippet` text,
  PRIMARY KEY (`id`),
  KEY `repo_id` (`repo_id`),
  CONSTRAINT `REPO_EMBEDDING_ibfk_1` FOREIGN KEY (`repo_id`) REFERENCES `GITHUB_REPO` (`repo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `REPO_FAV` (
  `fav_repo_id` int NOT NULL AUTO_INCREMENT COMMENT '즐겨찾기 레포 번호',
  `user_id` int NOT NULL COMMENT '유저 고유 번호',
  `repo_id` int NOT NULL COMMENT '레포 고유 번호',
  `created_at` date NOT NULL COMMENT '등록일',
  PRIMARY KEY (`fav_repo_id`),
  KEY `user_id` (`user_id`),
  KEY `repo_id` (`repo_id`),
  CONSTRAINT `REPO_FAV_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`),
  CONSTRAINT `REPO_FAV_ibfk_2` FOREIGN KEY (`repo_id`) REFERENCES `GITHUB_REPO` (`repo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `USER_REPO` (
  `user_repo_id` int NOT NULL AUTO_INCREMENT COMMENT '레포 사용 번호',
  `user_id` int NOT NULL COMMENT '유저 고유 번호',
  `repo_id` int NOT NULL COMMENT '레포 고유 번호',
  `created_at` date NOT NULL COMMENT '연동일',
  PRIMARY KEY (`user_repo_id`),
  UNIQUE KEY `uk_user_repo` (`user_id`,`repo_id`),
  KEY `repo_id` (`repo_id`),
  CONSTRAINT `USER_REPO_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `USER` (`user_id`),
  CONSTRAINT `USER_REPO_ibfk_2` FOREIGN KEY (`repo_id`) REFERENCES `GITHUB_REPO` (`repo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ad-hoc test table, not part of the domain model
CREATE TABLE `test_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
