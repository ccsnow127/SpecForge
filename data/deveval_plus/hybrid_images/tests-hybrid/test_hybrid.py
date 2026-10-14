import pytest
import numpy as np
from hybrid import (
    cross_correlation_2d,
    convolve_2d,
    gaussian_blur_kernel_2d,
    low_pass,
    high_pass,
    create_hybrid_image,
)


class TestCrossCorrelation2d:
    def test_identity_kernel_grayscale(self):
        img = np.array([[1, 2, 3],
                        [4, 5, 6],
                        [7, 8, 9]], dtype=np.float64)
        kernel = np.array([[0, 0, 0],
                           [0, 1, 0],
                           [0, 0, 0]], dtype=np.float64)
        result = cross_correlation_2d(img, kernel)
        np.testing.assert_array_almost_equal(result.squeeze(), img)

    def test_output_shape_grayscale(self):
        img = np.ones((5, 5), dtype=np.float64)
        kernel = np.ones((3, 3), dtype=np.float64)
        result = cross_correlation_2d(img, kernel)
        assert result.shape[0] == 5
        assert result.shape[1] == 5

    def test_output_shape_rgb(self):
        img = np.ones((5, 5, 3), dtype=np.float64)
        kernel = np.ones((3, 3), dtype=np.float64)
        result = cross_correlation_2d(img, kernel)
        assert result.shape == (5, 5, 3)

    def test_uniform_kernel_sums(self):
        img = np.ones((5, 5), dtype=np.float64)
        kernel = np.ones((3, 3), dtype=np.float64) / 9.0
        result = cross_correlation_2d(img, kernel)
        center = result[2, 2]
        np.testing.assert_almost_equal(center, 1.0, decimal=5)


class TestConvolve2d:
    def test_symmetric_kernel_same_as_correlation(self):
        img = np.random.rand(5, 5)
        kernel = np.array([[1, 1, 1],
                           [1, 1, 1],
                           [1, 1, 1]], dtype=np.float64)
        corr = cross_correlation_2d(img, kernel)
        conv = convolve_2d(img, kernel)
        np.testing.assert_array_almost_equal(corr, conv)

    def test_output_shape(self):
        img = np.ones((7, 7), dtype=np.float64)
        kernel = np.ones((3, 3), dtype=np.float64)
        result = convolve_2d(img, kernel)
        assert result.shape[0] == 7
        assert result.shape[1] == 7

    def test_non_symmetric_kernel_differs(self):
        img = np.array([[0, 0, 0, 0, 0],
                        [0, 0, 0, 0, 0],
                        [0, 0, 1, 0, 0],
                        [0, 0, 0, 0, 0],
                        [0, 0, 0, 0, 0]], dtype=np.float64)
        kernel = np.array([[1, 0, 0],
                           [0, 0, 0],
                           [0, 0, 0]], dtype=np.float64)
        corr = cross_correlation_2d(img, kernel)
        conv = convolve_2d(img, kernel)
        assert not np.allclose(corr, conv)


class TestGaussianBlurKernel2d:
    def test_kernel_shape(self):
        kernel = gaussian_blur_kernel_2d(1.0, 5, 5)
        assert kernel.shape == (5, 5)

    def test_kernel_sums_to_one(self):
        kernel = gaussian_blur_kernel_2d(1.0, 5, 5)
        np.testing.assert_almost_equal(np.sum(kernel), 1.0, decimal=5)

    def test_center_is_max(self):
        kernel = gaussian_blur_kernel_2d(1.0, 5, 5)
        assert kernel[2, 2] == np.max(kernel)

    def test_non_square_kernel(self):
        kernel = gaussian_blur_kernel_2d(1.0, 3, 5)
        assert kernel.shape == (3, 5)

    def test_larger_sigma_wider_spread(self):
        k1 = gaussian_blur_kernel_2d(0.5, 5, 5)
        k2 = gaussian_blur_kernel_2d(2.0, 5, 5)
        assert k1[2, 2] > k2[2, 2]


class TestLowPass:
    def test_output_shape(self):
        img = np.ones((10, 10), dtype=np.float64)
        result = low_pass(img, 1.0, 3)
        assert result.shape[0] == 10
        assert result.shape[1] == 10

    def test_uniform_image_unchanged(self):
        img = np.ones((10, 10), dtype=np.float64) * 128
        result = low_pass(img, 1.0, 3)
        center = result[5, 5]
        np.testing.assert_almost_equal(center, 128.0, decimal=1)


class TestHighPass:
    def test_output_shape(self):
        img = np.ones((10, 10), dtype=np.float64)
        result = high_pass(img, 1.0, 3)
        assert result.shape[0] == 10
        assert result.shape[1] == 10

    def test_uniform_image_near_zero(self):
        img = np.ones((10, 10), dtype=np.float64) * 128
        result = high_pass(img, 1.0, 3)
        center = result[5, 5]
        np.testing.assert_almost_equal(center, 0.0, decimal=1)


class TestCreateHybridImage:
    def test_output_shape(self):
        img1 = np.ones((10, 10, 3), dtype=np.float32) * 0.5
        img2 = np.ones((10, 10, 3), dtype=np.float32) * 0.5
        result = create_hybrid_image(img1, img2, 1.0, 3, "low", 1.0, 3, "high", 0.5)
        assert result.shape == (10, 10, 3)

    def test_output_dtype_uint8(self):
        img1 = np.ones((10, 10, 3), dtype=np.uint8) * 128
        img2 = np.ones((10, 10, 3), dtype=np.uint8) * 128
        result = create_hybrid_image(img1, img2, 1.0, 3, "low", 1.0, 3, "high", 0.5)
        assert result.dtype == np.uint8

    def test_output_clipped(self):
        img1 = np.ones((10, 10, 3), dtype=np.float32) * 0.5
        img2 = np.ones((10, 10, 3), dtype=np.float32) * 0.5
        result = create_hybrid_image(img1, img2, 1.0, 3, "low", 1.0, 3, "high", 0.5)
        assert result.min() >= 0
        assert result.max() <= 255

    def test_case_insensitive_high_low(self):
        img1 = np.ones((10, 10, 3), dtype=np.float32) * 0.5
        img2 = np.ones((10, 10, 3), dtype=np.float32) * 0.5
        result = create_hybrid_image(img1, img2, 1.0, 3, "LOW", 1.0, 3, "HIGH", 0.5)
        assert result.shape == (10, 10, 3)
