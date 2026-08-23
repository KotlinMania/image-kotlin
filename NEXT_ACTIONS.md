# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 24/71 (33.8%)
- **Function parity:** 74/1559 matched (target 178) — 4.7%
- **Class/type parity:** 29/279 matched (target 96) — 10.4%
- **Combined symbol parity:** 103/1838 matched (target 274) — 5.6%
- **Average inline-code cosine:** 0.31 (function body across 12 matched files)
- **Average documentation cosine:** 0.60 (doc text across 12 matched files)
- **Cheat-zeroed Files:** 16
- **Critical Issues:** 22 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. error

- **Target:** `image.Error`
- **Similarity:** 0.26
- **Dependents:** 3
- **Priority Score:** 3072307.5
- **Functions:** 5/11 matched (target 25)
- **Missing functions:** `new`, `from`, `fmt`, `source`, `test_send_sync_stability`, `assert_send_sync`
- **Types:** 11/12 matched (target 35)
- **Missing types:** `ImageResult`
- **Tests:** 0/2 matched

### 2. io

- **Target:** `io.Mod [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 3
- **Priority Score:** 3050510.0
- **Functions:** 0/1 matched (target 0)
- **Missing functions:** `read_exact_vec`
- **Types:** 0/4 matched (target 0)
- **Missing types:** `Reader`, `Limits`, `LimitSupport`, `ReadExt`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/io.rs` vs expected `io.rs`
- **Proposed provenance header:** `// port-lint: source io.rs` (current: `// port-lint: source src/io.rs`)
- **Lint issues:** 1

### 3. math.rect

- **Target:** `math.Rect`
- **Similarity:** 1.00
- **Dependents:** 3
- **Priority Score:** 3000100.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 4. pnm.autobreak

- **Target:** `pnm.AutoBreak`
- **Similarity:** 0.54
- **Dependents:** 2
- **Priority Score:** 2020804.6
- **Functions:** 5/7 matched (target 6)
- **Missing functions:** `new`, `drop`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 2/2 matched

### 5. metadata.cicp

- **Target:** `metadata.Cicp`
- **Similarity:** 0.06
- **Dependents:** 1
- **Priority Score:** 1384909.4
- **Functions:** 4/36 matched (target 12)
- **Missing functions:** `to_moxcms`, `new`, `supported_transform_fn`, `check_applicable`, `build_transforms`, `transform_dynamic`, `select_transform_u8`, `select_transform_u16`, `select_transform_f32`, `expand_luma_rgb`, `expand_luma_rgba`, `expand_rgb`, `expand_rgba`, `clamp_rgb`, `clamp_rgba`, `clamp_rgb_luma`, `clamp_rgba_luma`, `cast_pixels`, `cast_pixels_by_fallback`, `cast_pixels_from_subpixels`, `expand_to_f32`, `clamp_from_f32`, `select_transform`, `to_moxcms_compute_profile`, `from`, `map_layout`, `moxcms`, `can_create_transforms`, `no_coefficient_fallback`, `transform_pixels_srgb`, `transform_pixels_srgb_16`, `transform_pixels_srgb_luma_alpha`
- **Types:** 7/13 matched (target 8)
- **Missing types:** `CicpTransform`, `CicpApplicable`, `RgbTransforms`, `CicpPixelCast`, `ColorComponentForCicp`, `ColorProfile`
- **Tests:** 0/6 matched

### 6. io.limits

- **Target:** `io.Limits`
- **Similarity:** 0.42
- **Dependents:** 1
- **Priority Score:** 1031105.8
- **Functions:** 6/9 matched (target 11)
- **Missing functions:** `default`, `reserve_usize`, `free_usize`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 7. tga.header

- **Target:** `tga.Header`
- **Similarity:** 0.48
- **Dependents:** 1
- **Priority Score:** 1010905.1
- **Functions:** 6/7 matched (target 10)
- **Missing functions:** `new`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 8. jpeg.transform

- **Target:** `jpeg.Transform [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1000110.0
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/codecs/jpeg/transform.rs` vs expected `codecs/jpeg/transform.rs`
- **Proposed provenance header:** `// port-lint: source codecs/jpeg/transform.rs` (current: `// port-lint: source src/codecs/jpeg/transform.rs`)
- **Lint issues:** 1

### 9. imageops.mod

- **Target:** `utils.ExpandBitsTests [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 262610.0
- **Functions:** 0/26 matched (target 2)
- **Missing functions:** `crop`, `crop_imm`, `crop_dimms`, `overlay_bounds`, `overlay_bounds_ext`, `overlay`, `tile`, `vertical_gradient`, `horizontal_gradient`, `replace`, `test_overlay_bounds_ext`, `test_image_in_image`, `test_image_in_image_outside_of_bounds`, `test_image_outside_image_no_wrap_around`, `test_image_coordinate_overflow`, `test_image_horizontal_gradient_limits`, `test_image_vertical_gradient_limits`, `test_blur_zero`, `test_fast_blur_zero`, `test_fast_blur_negative`, `test_fast_large_sigma`, `test_fast_blur_empty`, `test_fast_blur_3_channels`, `test_fast_blur_2_channels`, `test_fast_blur_1_channels`, `fast_blur_approximates_gaussian_blur_well`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 0/16 matched
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `imageops/mod.rs`
- **Proposed provenance header:** `// port-lint: source imageops/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 10. color

- **Target:** `image.Color`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 143607.1
- **Functions:** 20/28 matched (target 54)
- **Missing functions:** `try_from`, `from_primitive`, `normalize_float`, `into_color`, `rgb_to_luma`, `from_color`, `invert`, `test_lossless_conversions`
- **Types:** 2/8 matched (target 35)
- **Missing types:** `Error`, `FromPrimitive`, `FromColor`, `IntoColor`, `Blend`, `Invert`
- **Tests:** 11/12 matched

### 11. utils.mod

- **Target:** `utils.Mod [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 70710.0
- **Functions:** 0/7 matched (target 0)
- **Missing functions:** `expand_packed`, `expand_bits`, `check_dimension_overflow`, `vec_copy_to_u8`, `clamp`, `vec_try_with_capacity`, `gray_to_luma8_skip`
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Tests:** 0/1 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/utils/mod.rs` vs expected `utils/mod.rs`
- **Proposed provenance header:** `// port-lint: source utils/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 12. pnm.mod

- **Target:** `pnm.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 60610.0
- **Functions:** 0/6 matched (target 0)
- **Missing functions:** `execute_roundtrip_default`, `execute_roundtrip_with_subtype`, `execute_roundtrip_u16`, `roundtrip_gray`, `roundtrip_rgb`, `roundtrip_u16`
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Tests:** 0/6 matched

### 13. io.format

- **Target:** `io.Format [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 11410.0
- **Functions:** 12/13 matched (target 16)
- **Missing functions:** `inner`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 2/2 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/io/format.rs` vs expected `io/format.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `tests:src/io/format.rs` vs expected `io/format.rs`
- **Proposed provenance header:** `// port-lint: source io/format.rs` (current: `// port-lint: source src/io/format.rs`)
- **Proposed provenance header:** `// port-lint: tests io/format.rs` (current: `// port-lint: tests src/io/format.rs`)
- **Lint issues:** 2

### 14. metadata

- **Target:** `metadata.Mod [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10910.0
- **Functions:** 6/7 matched (target 22)
- **Missing functions:** `test_extraction_and_clearing`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/metadata.rs` vs expected `metadata.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/metadata.rs` vs expected `metadata.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/metadata.rs` vs expected `metadata.rs`
- **Proposed provenance header:** `// port-lint: source metadata.rs` (current: `// port-lint: source src/metadata.rs`)
- **Proposed provenance header:** `// port-lint: source metadata.rs` (current: `// port-lint: source src/metadata.rs`)
- **Proposed provenance header:** `// port-lint: source metadata.rs` (current: `// port-lint: source src/metadata.rs`)
- **Lint issues:** 3

### 15. math.utils

- **Target:** `math.Utils`
- **Similarity:** 0.73
- **Dependents:** 0
- **Priority Score:** 702.7
- **Functions:** 7/7 matched (target 10)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 5/5 matched

### 16. jpeg.entropy

- **Target:** `jpeg.Entropy [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 210.0
- **Functions:** 2/2 matched (target 4)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/codecs/jpeg/entropy.rs` vs expected `codecs/jpeg/entropy.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/codecs/jpeg/entropy.rs` vs expected `codecs/jpeg/entropy.rs`
- **Proposed provenance header:** `// port-lint: source codecs/jpeg/entropy.rs` (current: `// port-lint: source src/codecs/jpeg/entropy.rs`)
- **Proposed provenance header:** `// port-lint: source codecs/jpeg/entropy.rs` (current: `// port-lint: source src/codecs/jpeg/entropy.rs`)
- **Lint issues:** 2

### 17. tga.mod

- **Target:** `tga.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 18. jpeg.mod

- **Target:** `jpeg.Mod [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/codecs/jpeg/mod.rs` vs expected `codecs/jpeg/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/jpeg/mod.rs` (current: `// port-lint: source src/codecs/jpeg/mod.rs`)
- **Lint issues:** 1

### 19. avif.mod

- **Target:** `utils.Clamp [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `codecs/avif/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/avif/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 20. bmp.mod

- **Target:** `utils.ExpandBits [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `codecs/bmp/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/bmp/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 21. hdr.mod

- **Target:** `utils.ExpandPacked [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `codecs/hdr/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/hdr/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 22. math.mod

- **Target:** `math.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 23. ico.mod

- **Target:** `utils.VecTryWithCapacity [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `codecs/ico/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/ico/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 24. webp.mod

- **Target:** `utils.CheckDimensionOverflow [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `codecs/webp/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/webp/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

