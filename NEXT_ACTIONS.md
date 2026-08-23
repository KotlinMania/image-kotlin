# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 41/71 (57.7%)
- **Function parity:** 181/1472 matched (target 358) — 12.3%
- **Class/type parity:** 60/279 matched (target 163) — 21.5%
- **Combined symbol parity:** 241/1751 matched (target 521) — 13.8%
- **Average inline-code cosine:** 0.25 (function body across 29 matched files)
- **Average documentation cosine:** 0.51 (doc text across 29 matched files)
- **Cheat-zeroed Files:** 16
- **Critical Issues:** 39 files with <0.60 function similarity

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

### 5. images.flat

- **Target:** `images.Flat`
- **Similarity:** 0.04
- **Dependents:** 1
- **Priority Score:** 1556109.6
- **Functions:** 4/51 matched (target 10)
- **Missing functions:** `extents`, `bounds`, `min_length`, `fits`, `increasing_stride_dims`, `has_aliased_samples`, `is_normal`, `in_bounds`, `index`, `index_ignoring_bounds`, `in_bounds_index`, `shrink_to`, `stride`, `checked_len`, `len`, `as_ref`, `as_mut`, `to_vec`, `get_mut_sample`, `as_view`, `as_view_with_mut_samples`, `as_view_mut`, `as_slice`, `as_mut_slice`, `image_slice`, `image_mut_slice`, `try_into_buffer`, `with_monocolor`, `into_inner`, `flat`, `samples`, `try_upgrade`, `panic_cwh_out_of_bounds`, `panic_pixel_out_of_bounds`, `index_mut`, `dimensions`, `get_pixel`, `get_pixel_mut`, `put_pixel`, `blend_pixel`, `from`, `fmt`, `partial_cmp`, `aliasing_view`, `mutable_view`, `normal_forms`, `image_buffer_conversion`
- **Types:** 2/10 matched (target 3)
- **Missing types:** `Dim`, `View`, `ViewMut`, `Error`, `NormalForm`, `Output`, `Pixel`, `NormalFormRequiredError`
- **Tests:** 0/4 matched

### 6. metadata.cicp

- **Target:** `metadata.Cicp`
- **Similarity:** 0.06
- **Dependents:** 1
- **Priority Score:** 1384909.4
- **Functions:** 4/36 matched (target 12)
- **Missing functions:** `to_moxcms`, `new`, `supported_transform_fn`, `check_applicable`, `build_transforms`, `transform_dynamic`, `select_transform_u8`, `select_transform_u16`, `select_transform_f32`, `expand_luma_rgb`, `expand_luma_rgba`, `expand_rgb`, `expand_rgba`, `clamp_rgb`, `clamp_rgba`, `clamp_rgb_luma`, `clamp_rgba_luma`, `cast_pixels`, `cast_pixels_by_fallback`, `cast_pixels_from_subpixels`, `expand_to_f32`, `clamp_from_f32`, `select_transform`, `to_moxcms_compute_profile`, `from`, `map_layout`, `moxcms`, `can_create_transforms`, `no_coefficient_fallback`, `transform_pixels_srgb`, `transform_pixels_srgb_16`, `transform_pixels_srgb_luma_alpha`
- **Types:** 7/13 matched (target 8)
- **Missing types:** `CicpTransform`, `CicpApplicable`, `RgbTransforms`, `CicpPixelCast`, `ColorComponentForCicp`, `ColorProfile`
- **Tests:** 0/6 matched

### 7. images.sub_image

- **Target:** `images.SubImage`
- **Similarity:** 0.09
- **Dependents:** 1
- **Priority Score:** 1192409.1
- **Functions:** 4/18 matched (target 6)
- **Missing functions:** `new`, `to_image`, `view`, `inner`, `sub_image`, `inner_mut`, `deref`, `deref_mut`, `buffer_with_dimensions`, `get_pixel_mut`, `put_pixel`, `blend_pixel`, `preserves_color_space`, `deep_preserves_color_space`
- **Types:** 1/6 matched (target 1)
- **Missing types:** `SubImageInner`, `DerefPixel`, `DerefSubpixel`, `Target`, `Pixel`
- **Tests:** 0/2 matched

### 8. imageops.fast_blur

- **Target:** `imageops.FastBlur`
- **Similarity:** 0.17
- **Dependents:** 1
- **Priority Score:** 1121508.2
- **Functions:** 3/14 matched (target 8)
- **Missing functions:** `test_radius_size`, `rounding_saturating_mul`, `box_blur_horizontal_pass_strategy`, `box_blur_vertical_pass_strategy`, `box_blur_horizontal_pass_impl`, `box_blur_vertical_pass_impl`, `new`, `next_u32`, `next_u8`, `next_f32_in_range`, `test_box_blur`
- **Types:** 0/1 matched
- **Missing types:** `Rng`
- **Tests:** 0/5 matched

### 9. io.free_functions

- **Target:** `io.FreeFunctions`
- **Similarity:** 0.34
- **Dependents:** 1
- **Priority Score:** 1111706.6
- **Functions:** 5/16 matched (target 12)
- **Missing functions:** `load`, `save_buffer`, `save_buffer_with_format`, `decoder_to_vec`, `dimensions`, `color_type`, `read_image`, `read_image_boxed`, `seek_scanline`, `read_scanline`, `test_load_rect_single_scanline`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Tests:** 1/8 matched

### 10. io.limits

- **Target:** `io.Limits`
- **Similarity:** 0.42
- **Dependents:** 1
- **Priority Score:** 1031105.8
- **Functions:** 6/9 matched (target 11)
- **Missing functions:** `default`, `reserve_usize`, `free_usize`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 11. tga.header

- **Target:** `tga.Header`
- **Similarity:** 0.48
- **Dependents:** 1
- **Priority Score:** 1010905.1
- **Functions:** 6/7 matched (target 10)
- **Missing functions:** `new`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 12. jpeg.transform

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

### 13. imageops.sample

- **Target:** `imageops.Sample`
- **Similarity:** 0.04
- **Dependents:** 0
- **Priority Score:** 626809.6
- **Functions:** 5/63 matched (target 9)
- **Missing functions:** `to_i8`, `to_i16`, `to_i64`, `to_u8`, `to_u16`, `to_u64`, `to_f64`, `bc_cubic_spline`, `gaussian`, `lanczos3_kernel`, `catmullrom_kernel`, `triangle_kernel`, `box_kernel`, `horizontal_sample`, `sample_bilinear`, `sample_nearest`, `interpolate_nearest`, `interpolate_bilinear`, `vertical_sample`, `zeroed`, `sample_val`, `add_pixel`, `thumbnail_sample_block`, `thumbnail_sample_fraction_horizontal`, `thumbnail_sample_fraction_vertical`, `thumbnail_sample_fraction_both`, `filter3x3`, `blur`, `blur_advanced`, `get_gaussian_kernel_1d`, `new_from_radius`, `new_from_kernel_size`, `new_anisotropic_kernel_size`, `new_from_sigma`, `round_to_nearest_odd`, `sigma_size`, `kernel_size_from_sigma`, `gaussian_blur_dyn_image`, `gaussian_blur_indirect`, `gaussian_blur_indirect_impl`, `unsharpen`, `bench_resize`, `test_resize_same_size`, `test_sample_bilinear`, `test_sample_nearest`, `test_sample_bilinear_correctness`, `bench_sample_bilinear`, `test_sample_nearest_correctness`, `bench_resize_same_size`, `test_issue_186`, `bench_thumbnail`, `bench_thumbnail_upsize`, `bench_thumbnail_upsize_irregular`, `resize_transparent_image`, `assert_resize`, `bug_1600`, `issue_2340`, `issue_2340_refl`
- **Types:** 1/5 matched (target 2)
- **Missing types:** `Filter`, `FloatNearest`, `ThumbnailSum`, `GaussianBlurParameters`
- **Tests:** 0/17 matched

### 14. imageops.filter_1d

- **Target:** `imageops.Filter1d`
- **Similarity:** 0.03
- **Dependents:** 0
- **Priority Score:** 263009.7
- **Functions:** 2/23 matched (target 4)
- **Missing functions:** `make_arena_row`, `make_columns_arenas`, `to_`, `filter_symmetric_column`, `filter_symmetric_row`, `transform`, `prepare_symmetric_kernel`, `filter_2d_separable_ring_queue`, `filter_2d_separable`, `filter_2d_sep_plane`, `filter_2d_sep_la`, `filter_2d_sep_rgb`, `filter_2d_sep_rgba`, `filter_2d_sep_la_f32`, `filter_2d_sep_plane_f32`, `filter_2d_sep_rgb_f32`, `filter_2d_sep_rgba_f32`, `filter_2d_sep_rgb_u16`, `filter_2d_sep_rgba_u16`, `filter_2d_sep_la_u16`, `filter_2d_sep_plane_u16`
- **Types:** 2/7 matched (target 2)
- **Missing types:** `SafeMul`, `SafeAdd`, `ArenaColumns`, `ToStorage`, `KernelTransformer`

### 15. imageops.mod

- **Target:** `utils.ExpandPacked [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 262610.0
- **Functions:** 0/26 matched (target 1)
- **Missing functions:** `crop`, `crop_imm`, `crop_dimms`, `overlay_bounds`, `overlay_bounds_ext`, `overlay`, `tile`, `vertical_gradient`, `horizontal_gradient`, `replace`, `test_overlay_bounds_ext`, `test_image_in_image`, `test_image_in_image_outside_of_bounds`, `test_image_outside_image_no_wrap_around`, `test_image_coordinate_overflow`, `test_image_horizontal_gradient_limits`, `test_image_vertical_gradient_limits`, `test_blur_zero`, `test_fast_blur_zero`, `test_fast_blur_negative`, `test_fast_large_sigma`, `test_fast_blur_empty`, `test_fast_blur_3_channels`, `test_fast_blur_2_channels`, `test_fast_blur_1_channels`, `fast_blur_approximates_gaussian_blur_well`
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Tests:** 0/16 matched
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `imageops/mod.rs`
- **Proposed provenance header:** `// port-lint: source imageops/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 16. animation

- **Target:** `image.Animation`
- **Similarity:** 0.22
- **Dependents:** 0
- **Priority Score:** 233707.8
- **Functions:** 10/32 matched (target 23)
- **Missing functions:** `new`, `clone`, `clone_from`, `from_parts`, `buffer_mut`, `into_buffer`, `from_saturating_duration`, `from_ratio`, `into_ratio`, `closest_bounded_fraction`, `compare_fraction`, `abs_diff_nom`, `from`, `eq`, `partial_cmp`, `cmp`, `simple`, `fps_30`, `duration_outlier`, `duration_approx`, `precise`, `small`
- **Types:** 4/5 matched
- **Missing types:** `Item`
- **Tests:** 0/6 matched

### 17. imageops.colorops

- **Target:** `imageops.Colorops`
- **Similarity:** 0.12
- **Dependents:** 0
- **Priority Score:** 202808.8
- **Functions:** 8/24 matched (target 9)
- **Missing functions:** `grayscale_alpha`, `grayscale_with_type`, `grayscale_with_type_alpha`, `contrast_in_place`, `brighten_in_place`, `huerotate_in_place`, `lookup`, `has_lookup`, `index_of`, `map_color`, `diffuse_err`, `dither`, `index_colors`, `test_dither`, `test_brighten_place`, `pixel_diffs`
- **Types:** 0/4 matched (target 1)
- **Missing types:** `Subpixel`, `ColorMap`, `BiLevel`, `Color`
- **Tests:** 3/6 matched
- **Lint issues:** 4

### 18. codecs.farbfeld

- **Target:** `codecs.Farbfeld`
- **Similarity:** 0.15
- **Dependents:** 0
- **Priority Score:** 182508.5
- **Functions:** 5/22 matched (target 8)
- **Missing functions:** `new`, `read_dimm`, `read`, `seek`, `parse_offset`, `consume_channel`, `cache_byte`, `read_image_boxed`, `read_rect`, `encode_impl`, `read_rect_1x2`, `read_rect_2x2`, `read_rect_2x1`, `read_rect_2x3`, `read_rect_in_stream`, `dimension_overflow`, `degenerate_pixels`
- **Types:** 2/3 matched
- **Missing types:** `FarbfeldReader`
- **Tests:** 0/7 matched

### 19. traits

- **Target:** `image.Traits`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 162609.2
- **Functions:** 4/13 matched (target 17)
- **Missing functions:** `from`, `channels`, `layout`, `dispatch_transform_from_sealed`, `double_dispatch_transform_from_sealed`, `transform_on`, `alpha`, `map_without_alpha`, `apply_without_alpha`
- **Types:** 6/13 matched (target 27)
- **Missing types:** `Larger`, `Ratio`, `LayoutWithColor`, `PrivateToken`, `SealedPixelWithColorType`, `TransformableSubpixel`, `HelpDispatchTransform`

### 20. color

- **Target:** `image.Color`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 143607.1
- **Functions:** 20/28 matched (target 54)
- **Missing functions:** `try_from`, `from_primitive`, `normalize_float`, `into_color`, `rgb_to_luma`, `from_color`, `invert`, `test_lossless_conversions`
- **Types:** 2/8 matched (target 35)
- **Missing types:** `Error`, `FromPrimitive`, `FromColor`, `IntoColor`, `Blend`, `Invert`
- **Tests:** 11/12 matched

### 21. imageops.affine

- **Target:** `imageops.Affine`
- **Similarity:** 0.20
- **Dependents:** 0
- **Priority Score:** 132208.0
- **Functions:** 9/22 matched (target 9)
- **Missing functions:** `rotate90_in`, `rotate180_in`, `rotate270_in`, `flip_horizontal_in`, `flip_vertical_in`, `rotate180_in_place`, `flip_horizontal_in_place`, `flip_vertical_in_place`, `test_rotate270`, `test_rotate180_in_place`, `test_flip_horizontal_in_place`, `test_flip_vertical_in_place`, `pixel_diffs`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 4/9 matched

### 22. io.image_reader_type

- **Target:** `io.ImageReaderTest`
- **Similarity:** 0.26
- **Dependents:** 0
- **Priority Score:** 111907.4
- **Functions:** 7/17 matched (target 11)
- **Missing functions:** `new`, `no_limits`, `limits`, `into_inner`, `make_decoder`, `guess_format`, `into_dimensions`, `require_format`, `open`, `open_impl`
- **Types:** 1/2 matched
- **Missing types:** `Format`

### 23. tga.encoder

- **Target:** `tga.Encoder`
- **Similarity:** 0.46
- **Dependents:** 0
- **Priority Score:** 72805.4
- **Functions:** 18/25 matched
- **Missing functions:** `fmt`, `from`, `new`, `make_compatible_img`, `round_trip_image`, `round_trip_bw`, `round_trip_single_pixel_rgba`
- **Types:** 3/3 matched (target 7)
- **Missing types:** _none_
- **Tests:** 12/15 matched

### 24. io.decoder

- **Target:** `io.Decoder`
- **Similarity:** 0.16
- **Dependents:** 0
- **Priority Score:** 71708.4
- **Functions:** 8/13 matched (target 8)
- **Missing functions:** `dimensions`, `color_type`, `read_image`, `read_image_boxed`, `total_bytes_overflow`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `AnimationDecoder`, `D`
- **Tests:** 0/1 matched

### 25. utils.mod

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

### 26. pnm.mod

- **Target:** `pnm.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 60610.0
- **Functions:** 0/6 matched (target 0)
- **Missing functions:** `execute_roundtrip_default`, `execute_roundtrip_with_subtype`, `execute_roundtrip_u16`, `roundtrip_gray`, `roundtrip_rgb`, `roundtrip_u16`
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Tests:** 0/6 matched

### 27. codecs.qoi

- **Target:** `codecs.Qoi [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 51110.0
- **Functions:** 4/9 matched (target 8)
- **Missing functions:** `new`, `read_image_boxed`, `decoding_error`, `encoding_error`, `decode_test_image`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 28. io.encoder

- **Target:** `io.Encoder`
- **Similarity:** 0.28
- **Dependents:** 0
- **Priority Score:** 50807.2
- **Functions:** 2/5 matched (target 2)
- **Missing functions:** `make_compatible_img`, `write_image`, `dynimage_conversion_8bit`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `MethodSealedToImage`, `ImageEncoderBoxed`

### 29. tga.decoder

- **Target:** `tga.Decoder`
- **Similarity:** 0.51
- **Dependents:** 0
- **Priority Score:** 31504.9
- **Functions:** 9/12 matched (target 11)
- **Missing functions:** `new`, `fixup_orientation`, `read_image_boxed`
- **Types:** 3/3 matched
- **Missing types:** _none_

### 30. io.format

- **Target:** `io.Format [PROVENANCE-FALLBACK]`
- **Similarity:** 0.39
- **Dependents:** 0
- **Priority Score:** 11406.1
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

### 31. metadata

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

### 32. math.utils

- **Target:** `math.Utils`
- **Similarity:** 0.73
- **Dependents:** 0
- **Priority Score:** 702.7
- **Functions:** 7/7 matched (target 10)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 5/5 matched

### 33. jpeg.entropy

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

### 34. tga.mod

- **Target:** `tga.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 35. jpeg.mod

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

### 36. bmp.mod

- **Target:** `utils.Clamp [STUB] [PROVENANCE-FALLBACK]`
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

### 37. avif.mod

- **Target:** `utils.ExpandBits [STUB] [PROVENANCE-FALLBACK]`
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

### 38. math.mod

- **Target:** `math.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 39. webp.mod

- **Target:** `utils.ExpandBitsTests [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `src/utils/mod.rs` vs expected `codecs/webp/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/webp/mod.rs` (current: `// port-lint: source src/utils/mod.rs`)
- **Lint issues:** 1

### 40. hdr.mod

- **Target:** `utils.VecTryWithCapacity [STUB] [PROVENANCE-FALLBACK]`
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

### 41. ico.mod

- **Target:** `utils.CheckDimensionOverflow [STUB] [PROVENANCE-FALLBACK]`
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

