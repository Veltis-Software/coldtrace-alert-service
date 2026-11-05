
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incident_ai_resolution_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `approved_at` datetime(6) DEFAULT NULL,
  `approved_by` varchar(255) DEFAULT NULL,
  `corrective_action_draft` varchar(1000) NOT NULL,
  `escalation_reason` varchar(800) NOT NULL,
  `escalation_recommended` bit(1) NOT NULL,
  `escalation_urgency` varchar(120) NOT NULL,
  `final_corrective_action` varchar(1000) DEFAULT NULL,
  `final_resolution_notes` varchar(1000) DEFAULT NULL,
  `generated_at` datetime(6) NOT NULL,
  `incident_id` bigint NOT NULL,
  `model_name` varchar(160) NOT NULL,
  `model_provider` varchar(80) NOT NULL,
  `organization_id` bigint NOT NULL,
  `probable_cause` varchar(800) NOT NULL,
  `provider_metadata` varchar(1000) DEFAULT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `rejected_by` varchar(255) DEFAULT NULL,
  `rejection_reason` varchar(1000) DEFAULT NULL,
  `resolution_notes_draft` varchar(1000) NOT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `summary` varchar(500) NOT NULL,
  PRIMARY KEY (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incident_ai_resolution_plan_required_evidences` (
  `ai_resolution_plan_id` bigint NOT NULL,
  `required_evidence` varchar(400) NOT NULL,
  `position` int NOT NULL,
  PRIMARY KEY (`ai_resolution_plan_id`,`position`),
  CONSTRAINT `FK9vlvanalqwxu33xcb7ax4y84a` FOREIGN KEY (`ai_resolution_plan_id`) REFERENCES `incident_ai_resolution_plans` (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incident_ai_resolution_plan_steps` (
  `ai_resolution_plan_id` bigint NOT NULL,
  `action` varchar(400) NOT NULL,
  `expected_outcome` varchar(400) NOT NULL,
  `rationale` varchar(500) NOT NULL,
  `step_sequence` int NOT NULL,
  `position` int NOT NULL,
  PRIMARY KEY (`ai_resolution_plan_id`,`position`),
  CONSTRAINT `FK6bw799b78fj8rj63rg033frr8` FOREIGN KEY (`ai_resolution_plan_id`) REFERENCES `incident_ai_resolution_plans` (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incident_ai_resolution_plan_uncertainty_notes` (
  `ai_resolution_plan_id` bigint NOT NULL,
  `uncertainty_note` varchar(500) NOT NULL,
  `position` int NOT NULL,
  PRIMARY KEY (`ai_resolution_plan_id`,`position`),
  CONSTRAINT `FKtniibe7jyvstq5hj1ws3w2mxr` FOREIGN KEY (`ai_resolution_plan_id`) REFERENCES `incident_ai_resolution_plans` (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
