"""Generate local administrator credentials without printing secret values.

Requires the bcrypt Python package. The output directory must be ACL restricted
on Windows before copying the SQL to the authorized server.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import secrets

import bcrypt


PASSWORD_LENGTH = 24


def new_password() -> str:
    value = secrets.token_urlsafe(18)
    # PasswordLoginBody accepts 5–30 characters; keep generation inside it.
    if len(value) != PASSWORD_LENGTH or not 5 <= len(value) <= 30:
        raise ValueError("Generated password does not satisfy login length constraints")
    return value


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--rotate", action="store_true", help="Explicitly replace existing local credentials and prepare a rotation SQL file")
    args = parser.parse_args()
    local = Path(__file__).resolve().parent / "local"
    local.mkdir(mode=0o700, exist_ok=True)
    path = local / "administrator.json"
    if path.exists() and not args.rotate:
        raise SystemExit("Credentials already exist; use --rotate only for an authorized password rotation")
    password, coach_password = new_password(), new_password()
    payload = {
        "url": "https://t1.wedodiary.top",
        "username": "admin",
        "password": password,
        "coachInitialPassword": coach_password,
        "notice": "首次登录后请修改密码。",
        "coachPasswordNotice": "管理员创建教练后，请在系统用户管理为各教练重置独立密码，并单独交付本人。",
    }
    password_hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=12)).decode()
    sql = (
        f"UPDATE sys_user SET password='{password_hash}' WHERE user_id=1;\n"
        f"UPDATE sys_config SET config_value='{coach_password}', remark='随机初始密码，创建后为每位教练单独重置' WHERE config_key='sys.user.initPassword';\n"
    )
    if not args.rotate:
        sql += "UPDATE sys_user SET nick_name='T1管理员',email='',phonenumber='',login_ip='',status='0' WHERE user_id=1;\nUPDATE sys_user SET status='1' WHERE user_name IN ('test','test1');\nDELETE FROM sys_oss_config;\n"
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    path.chmod(0o600)
    sql_path = local / ("rotate-admin.sql" if args.rotate else "bootstrap-admin.sql")
    sql_path.write_text(sql, encoding="utf-8")
    sql_path.chmod(0o600)
    if args.rotate:
        # An old bootstrap file would contain passwords that no longer match
        # administrator.json, and must not be reused for a fresh installation.
        (local / "bootstrap-admin.sql").unlink(missing_ok=True)
    print(f"Prepared {PASSWORD_LENGTH}-character credentials in {path}")
    print(f"SQL: {sql_path}; after applying a rotation, clear the sys_config cache through the administrator API")


if __name__ == "__main__":
    main()
