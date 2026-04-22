-- ============================================================================
-- V21 — Tables Projects et Project Tasks
-- ============================================================================
-- Projets et tâches synchronisables avec le système externe.
-- Visibles dans le dashboard client LMP.

CREATE TABLE IF NOT EXISTS projects (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    percent_complete      INTEGER      DEFAULT 0,
    expected_start_date   DATE,
    expected_end_date     DATE,
    actual_start_date     DATE,
    actual_end_date       DATE,
    customer_id           UUID         REFERENCES users(id),
    external_project_id   VARCHAR(140),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_tasks (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    priority              VARCHAR(20)  DEFAULT 'MEDIUM',
    progress              INTEGER      DEFAULT 0,
    expected_start_date   DATE,
    expected_end_date     DATE,
    project_id            UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    external_task_id      VARCHAR(140),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_projects_customer ON projects(customer_id);
CREATE INDEX IF NOT EXISTS idx_projects_external ON projects(external_project_id);
CREATE INDEX IF NOT EXISTS idx_project_tasks_project ON project_tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_project_tasks_external ON project_tasks(external_task_id);
