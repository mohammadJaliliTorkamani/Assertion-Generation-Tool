import io
import sys
import token
import tokenize
from rouge_score import rouge_scorer
import numpy as np
from itertools import product


def tokenize_assertion(assertion):
    tokens = []
    g = tokenize.generate_tokens(io.StringIO(assertion).readline)
    for tok in g:
        if tok.type in {token.NAME, token.OP, token.NUMBER, token.STRING}:
            tokens.append(tok.string)
    return tokens


def jaccard_similarity(set1, set2):
    intersection = len(set1.intersection(set2))
    union = len(set1.union(set2))
    return intersection / union if union != 0 else 0


# def levenshtein_distance(str1, str2):
#     tokens1 = tokenize_assertion(str1)
#     tokens2 = tokenize_assertion(str2)
#     len_tokens1, len_tokens2 = len(tokens1), len(tokens2)
#     dp = np.zeros((len_tokens1 + 1, len_tokens2 + 1))
#     for i in range(len_tokens1 + 1):
#         dp[i][0] = i
#     for j in range(len_tokens2 + 1):
#         dp[0][j] = j
#     for i in range(1, len_tokens1 + 1):
#         for j in range(1, len_tokens2 + 1):
#             cost = 0 if tokens1[i - 1] == tokens2[j - 1] else 1
#             dp[i][j] = min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
#     return dp[len_tokens1][len_tokens2], len_tokens1, len_tokens2


def compute(reference_path, candidate_path):
    # rouge = evaluate.load('rouge')
    # bleu = evaluate.load('bleu')  # Load BLEU metric
    scorer = rouge_scorer.RougeScorer(['rouge1', 'rouge2', 'rougeL'], use_stemmer=False)

    # with open(reference_path, 'r', encoding='utf-8') as ref_file:
    #     reference_lines = ref_file.readlines()
    # with open(candidate_path, 'r', encoding='utf-8') as candidate_file:
    #     candidate_lines = candidate_file.readlines()
    #
    # # Join the entire set of lines into single strings
    # reference_text = "\n".join(reference_lines).strip()
    # candidate_text = "\n".join(candidate_lines).strip()
    #
    # # Compute Rouge scores
    # rouge_scores = scorer.score(reference_text, candidate_text)
    #
    # # Compute Levenshtein distance
    # # lev_dist, len1, len2 = levenshtein_distance(reference_text, candidate_text)
    #
    # # Compute Jaccard similarity
    # tokens1, tokens2 = set(tokenize_assertion(reference_text)), set(tokenize_assertion(candidate_text))
    # jaccard_score = jaccard_similarity(tokens1, tokens2)

    # Read files
    with open(reference_path, 'r', encoding='utf-8') as ref_file:
        reference_lines = [line.strip() for line in ref_file.readlines() if line.strip()]
    with open(candidate_path, 'r', encoding='utf-8') as candidate_file:
        candidate_lines = [line.strip() for line in candidate_file.readlines() if line.strip()]

    # Initialize accumulators for each score type
    rouge_scores = {
        'rouge1': {'precision': [], 'recall': [], 'fmeasure': []},
        'rouge2': {'precision': [], 'recall': [], 'fmeasure': []},
        'rougeL': {'precision': [], 'recall': [], 'fmeasure': []}
    }
    jaccard_scores = []

    # Pairwise comparison
    for ref_line, cand_line in product(reference_lines, candidate_lines):
        # Compute Rouge scores
        scores = scorer.score(ref_line, cand_line)
        for key in ['rouge1', 'rouge2', 'rougeL']:
            rouge_scores[key]['precision'].append(scores[key].precision)
            rouge_scores[key]['recall'].append(scores[key].recall)
            rouge_scores[key]['fmeasure'].append(scores[key].fmeasure)

        # Compute Jaccard similarity
        tokens1, tokens2 = set(tokenize_assertion(ref_line)), set(tokenize_assertion(cand_line))
        jaccard_scores.append(jaccard_similarity(tokens1, tokens2))

    # Compute averages
    avg_rouge = {}
    for key in ['rouge1', 'rouge2', 'rougeL']:
        avg_rouge[key] = {
            'F': np.mean(rouge_scores[key]['fmeasure']),
            'P': np.mean(rouge_scores[key]['precision']),
            'R': np.mean(rouge_scores[key]['recall'])
        }

    avg_jaccard = np.mean(jaccard_scores)

    return {
        'rouge': {
            'rougeL': {
                'F': float(avg_rouge['rougeL']['F']),
                'P': float(avg_rouge['rougeL']['P']),
                'R': float(avg_rouge['rougeL']['R'])
            },
        },
        'jaccard': {'similarity': float(avg_jaccard)}
    }


if __name__ == "__main__":
    if len(sys.argv) == 3:
        reference_path = sys.argv[1]
        candidate_path = sys.argv[2]
    else:
        reference_path = 'ADDRESS_GOES_HERE'
        candidate_path = 'ADDRESS_GOES_HERE'

    result = compute(reference_path, candidate_path)
    print(result)
