"""
지역 마스터 데이터(regions) 시드 생성 스크립트.

소스: 행정표준코드관리시스템(https://www.code.go.kr) 법정동코드 전체자료 (KIKcd_B).
기준일: 2026-07-01 (광주·전남 통합, 인천 자치구 재편 반영).

규칙(2단계):
  - region_level_1 = 시/도, region_level_2 = 시/군/구
  - 일반시는 시 단위까지만 (예: 경기도 수원시). 광역시 자치구는 구까지.
  - 세종특별자치시는 하위 시군구가 없어 자기 자신을 region_level_2로 둔다.
  - '출장소'/'출장' 항목은 실 자치단체가 아니므로 제외.
  - full_name = region_level_1 + ' ' + region_level_2

사용:
  python3 scripts/generate_region_seed.py <KIKcd_B.xlsx 경로>
  → 표준출력으로 INSERT VALUES 목록을 뽑는다. (마이그레이션에 붙여넣기)
"""
import sys
import openpyxl


def load_regions(path):
    wb = openpyxl.load_workbook(path, read_only=True)
    ws = wb.active
    rows = ws.iter_rows(values_only=True)
    next(rows)  # header: 법정동코드, 시도명, 시군구명, 읍면동명, 동리명, 생성일자, 말소일자
    result = []
    seen = set()
    sido_with_child = set()
    active_sido = set()

    data = [r for r in rows]
    for r in data:
        code, sido, sgg, emd, ri, created, abolished = r[:7]
        if abolished:  # 말소된 항목 제외
            continue
        if sido and "출장" in sido:
            continue
        if sido and not sgg:
            active_sido.add(sido)
        if sgg:
            sido_with_child.add(sido)

    # 일반시 산하 구를 가진 부모 시 목록 (시군구명에 공백 있으면 "수원시 장안구" 형태)
    parents = {sgg.split(" ")[0] for r in data if not r[6] and r[2] and " " in r[2]}

    for r in data:
        code, sido, sgg, emd, ri, created, abolished = r[:7]
        if abolished or not sgg or emd or ri:
            continue
        if "출장" in sgg:
            continue
        # 안1: 일반시 구 → 시로 축약
        name = sgg.split(" ")[0] if " " in sgg else sgg
        key = (sido, name)
        if key in seen:
            continue
        seen.add(key)
        result.append(key)

    # 세종처럼 하위 시군구 없는 시도는 자기 자신
    for sido in sorted(active_sido):
        if sido not in sido_with_child:
            key = (sido, sido)
            if key not in seen:
                seen.add(key)
                result.append(key)
    return result


def main():
    if len(sys.argv) < 2:
        print("usage: python3 generate_region_seed.py <KIKcd_B.xlsx>", file=sys.stderr)
        sys.exit(1)
    regions = load_regions(sys.argv[1])
    lines = []
    for sido, sgg in regions:
        full = f"{sido} {sgg}"
        lines.append(f"    ('{sido}', '{sgg}', '{full}')")
    print(",\n".join(lines) + ";")
    print(f"-- rows: {len(regions)}", file=sys.stderr)


if __name__ == "__main__":
    main()
