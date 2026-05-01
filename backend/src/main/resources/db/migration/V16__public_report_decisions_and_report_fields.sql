create table if not exists public_report_decisions (
    id bigserial primary key,
    report_id bigint not null,
    action_type varchar(100) not null,
    reason text,
    created_at timestamptz not null default now()
);

alter table public_reports
    add column if not exists duplicate_of_report_id bigint;

alter table public_reports
    add column if not exists qualification_note text;

alter table public_reports
    add column if not exists decision_reason text;