import time
import random
from typing import List

class OcrLine:
    def __init__(self, text: str, left: int, top: int, right: int, bottom: int):
        self.text = text
        self.left = left
        self.top = top
        self.right = right
        self.bottom = bottom
        self.width = right - left
        self.height = bottom - top
        self.center_x = left + self.width / 2
        self.center_y = top + self.height / 2

def are_lines_groupable(l1: OcrLine, l2: OcrLine) -> bool:
    min_h = min(l1.height, l2.height)
    max_h = max(l1.height, l2.height)
    if min_h == 0 or max_h / min_h > 2.0:
        return False
        
    y_dist = abs(l1.center_y - l2.center_y)
    is_same_line = y_dist < min_h * 0.5
    
    if is_same_line:
        if l1.center_x < l2.center_x:
            gap = l2.left - l1.right
        else:
            gap = l1.left - l2.right
        if gap < min_h * 2.5 and gap > -min_h * 0.5:
            return True
            
    if l1.center_y < l2.center_y:
        v_gap = l2.top - l1.bottom
    else:
        v_gap = l1.top - l2.bottom
        
    is_adjacent_line = v_gap < min_h * 1.5 and v_gap > -min_h * 0.5
    
    if is_adjacent_line:
        overlap_left = max(l1.left, l2.left)
        overlap_right = min(l1.right, l2.right)
        x_overlap = overlap_right - overlap_left
        
        if x_overlap > -min_h * 0.5: 
            if v_gap < min_h * 1.0:
                return True
                
    return False

def sort_lines(lines: List[OcrLine]) -> List[OcrLine]:
    if not lines: return []
    sorted_by_top = sorted(lines, key=lambda l: l.top)
    rows = []
    current_row = [sorted_by_top[0]]
    current_bottom = sorted_by_top[0].bottom
    
    for line in sorted_by_top[1:]:
        if line.top < current_bottom - (line.height * 0.2):
            current_row.append(line)
            current_bottom = max(current_bottom, line.bottom)
        else:
            rows.append(current_row)
            current_row = [line]
            current_bottom = line.bottom
    rows.append(current_row)
    
    sorted_out = []
    for row in rows:
        sorted_out.extend(sorted(row, key=lambda l: l.left))
    return sorted_out

def reconstruct_text(lines: List[OcrLine], debug=False) -> str:
    if not lines:
        return ""
        
    sorted_lines = sort_lines(lines)
    groups = []
    
    for line in sorted_lines:
        merged = False
        for group in reversed(groups):
            last_line = group[-1]
            if are_lines_groupable(last_line, line):
                group.append(line)
                merged = True
                break
        
        if not merged:
            groups.append([line])
            
    result_lines = []
    for group in groups:
        text = " ".join(l.text for l in group)
        result_lines.append(text)
        
    if debug:
        print("--- DEBUG SPATIAL ---")
        for g in result_lines:
            print(f"GROUP: {g}")
            
    return "\n".join(result_lines)

def test_cases():
    cases = []
    # CASE A: THUMS UP
    cases.append({
        "id": "CASE-A",
        "lines": [
            OcrLine("THUMS", 10, 10, 60, 30),
            OcrLine("UP", 10, 32, 40, 52)
        ],
        "expected": "THUMS UP"
    })
    # CASE B: TH UMS UP
    cases.append({
        "id": "CASE-B",
        "lines": [
            OcrLine("TH", 10, 10, 30, 30),
            OcrLine("UMS", 35, 10, 70, 30),
            OcrLine("UP", 10, 32, 40, 52)
        ],
        "expected": "TH UMS UP"
    })
    # CASE C: JIM JAM
    cases.append({
        "id": "CASE-C",
        "lines": [
            OcrLine("JIM", 10, 10, 40, 30),
            OcrLine("JAM", 10, 32, 40, 52)
        ],
        "expected": "JIM JAM"
    })
    # CASE D: ORE O
    cases.append({
        "id": "CASE-D",
        "lines": [
            OcrLine("ORE", 10, 10, 40, 30),
            OcrLine("O", 10, 32, 20, 52)
        ],
        "expected": "ORE O"
    })
    # CASE E: Unrelated lines
    cases.append({
        "id": "CASE-E",
        "lines": [
            OcrLine("THUMS", 10, 10, 60, 30),
            OcrLine("UP", 10, 32, 40, 52),
            OcrLine("NET WEIGHT 500 ML", 10, 100, 150, 115),
            OcrLine("MRP Rs30", 10, 150, 80, 165)
        ],
        "expected": "THUMS UP\nNET WEIGHT 500 ML\nMRP Rs30"
    })
    # CASE F: Columnar MRP (MRP over 30, OFFER over 20)
    cases.append({
        "id": "CASE-F",
        "lines": [
            OcrLine("MRP", 10, 10, 40, 30),
            OcrLine("OFFER", 100, 10, 150, 30),
            OcrLine("30", 10, 35, 30, 55),
            OcrLine("20", 100, 35, 120, 55)
        ],
        "expected": "MRP 30\nOFFER 20"
    })
    # CASE G: Distant MRP and Price on same row (should not merge into same line, but fall through as separate lines)
    cases.append({
        "id": "CASE-G",
        "lines": [
            OcrLine("MRP", 10, 10, 40, 30),
            OcrLine("30.00", 200, 10, 250, 30)
        ],
        "expected": "MRP\n30.00"
    })
    # CASE H: Fragmented MRP label
    cases.append({
        "id": "CASE-H",
        "lines": [
            OcrLine("M", 10, 10, 20, 30),
            OcrLine("R", 25, 10, 35, 30),
            OcrLine("P", 40, 10, 50, 30),
            OcrLine("30", 60, 10, 80, 30)
        ],
        "expected": "M R P 30"
    })
    # CASE I: Small vertical offset (staggered)
    cases.append({
        "id": "CASE-I",
        "lines": [
            OcrLine("MRP", 10, 10, 40, 30),
            OcrLine("30", 45, 15, 65, 35)
        ],
        "expected": "MRP 30"
    })
    # CASE J: Moderate diagonal offset (should fail to merge, which is safe/expected)
    cases.append({
        "id": "CASE-J",
        "lines": [
            OcrLine("MRP", 10, 10, 40, 30),
            OcrLine("30", 50, 30, 70, 50)
        ],
        "expected": "MRP\n30"
    })
    
    correct = 0
    missed = 0
    incorrect = 0
    for c in cases:
        out = reconstruct_text(c["lines"])
        if out == c["expected"]:
            correct += 1
        else:
            print(f"FAILED {c['id']}")
            print(f"EXPECTED: {repr(c['expected'])}")
            print(f"GOT: {repr(out)}")
            incorrect += 1
            
    print(f"Spatial Basic Tests: {correct} Correct, {incorrect} Incorrect, {missed} Missed.")
    return correct, incorrect, missed

def stress_test():
    rng = random.Random(42)
    crashes = 0
    exceptions = 0
    
    # generate 500 random bounding box scenarios
    times = []
    
    for i in range(1000):
        lines = []
        num_lines = rng.randint(1, 20)
        for j in range(num_lines):
            left = rng.randint(0, 500)
            top = rng.randint(0, 500)
            w = rng.randint(20, 100)
            h = rng.randint(10, 40)
            lines.append(OcrLine(f"WORD{j}", left, top, left+w, top+h))
            
        start = time.perf_counter()
        try:
            res = reconstruct_text(lines)
        except Exception as e:
            crashes += 1
            exceptions += 1
        times.append((time.perf_counter() - start) * 1000)
        
    avg_t = sum(times)/len(times) if times else 0
    print(f"Stress Cases: 500 | Crashes: {crashes} | Exceptions: {exceptions} | Avg Latency: {avg_t:.2f} ms")

if __name__ == "__main__":
    test_cases()
    stress_test()
