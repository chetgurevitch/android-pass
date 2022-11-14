package me.proton.pass.presentation.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import me.proton.android.pass.autofill.api.AutofillStatus
import me.proton.android.pass.autofill.api.AutofillSupportedStatus
import me.proton.android.pass.autofill.fakes.TestAutofillManager
import me.proton.android.pass.biometry.BiometryResult
import me.proton.android.pass.biometry.ContextHolder
import me.proton.android.pass.biometry.TestBiometryManager
import me.proton.android.pass.notifications.fakes.TestSnackbarMessageRepository
import me.proton.android.pass.preferences.BiometricLockState
import me.proton.android.pass.preferences.HasAuthenticated
import me.proton.android.pass.preferences.HasCompletedOnBoarding
import me.proton.android.pass.preferences.TestPreferenceRepository
import me.proton.pass.common.api.None
import me.proton.pass.test.MainDispatcherRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnBoardingViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: OnBoardingViewModel
    private lateinit var snackbarMessageRepository: TestSnackbarMessageRepository
    private lateinit var preferenceRepository: TestPreferenceRepository
    private lateinit var biometryManager: TestBiometryManager
    private lateinit var autofillManager: TestAutofillManager

    @Before
    fun setUp() {
        snackbarMessageRepository = TestSnackbarMessageRepository()
        preferenceRepository = TestPreferenceRepository()
        biometryManager = TestBiometryManager()
        autofillManager = TestAutofillManager()
        viewModel = OnBoardingViewModel(
            autofillManager,
            biometryManager,
            preferenceRepository,
            snackbarMessageRepository
        )
    }

    @Test
    fun `sends correct initial state`() = runTest {
        viewModel.onBoardingUiState.test {
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial)
        }
    }

    @Test
    fun `given unsupported autofill should show 1 screen`() = runTest {
        viewModel.onBoardingUiState.test {
            skipItems(1)
            autofillManager.emitStatus(AutofillSupportedStatus.Unsupported)
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial.copy(pageCount = 1))
        }
    }

    @Test
    fun `given already enabled autofill should show 1 screen`() = runTest {
        viewModel.onBoardingUiState.test {
            skipItems(1)
            autofillManager.emitStatus(AutofillSupportedStatus.Supported(AutofillStatus.EnabledByOurService))
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial.copy(pageCount = 1))
        }
    }

    @Test
    fun `given a click on enable autofill should select next page`() = runTest {
        viewModel.onBoardingUiState.test {
            skipItems(1)
            viewModel.onMainButtonClick(OnBoardingPageName.Autofill, ContextHolder(None))
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial.copy(selectedPage = 1))
        }
    }

    @Test
    fun `given a click on skip autofill should select next page`() = runTest {
        viewModel.onBoardingUiState.test {
            skipItems(1)
            viewModel.onSkipButtonClick(OnBoardingPageName.Autofill)
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial.copy(selectedPage = 1))
        }
    }

    @Test
    fun `given a click on enable fingerprint should complete on boarding`() = runTest {
        viewModel.onBoardingUiState.test {
            skipItems(1)
            biometryManager.emitResult(BiometryResult.Success)
            preferenceRepository.setHasAuthenticated(HasAuthenticated.Authenticated)
            preferenceRepository.setBiometricLockState(BiometricLockState.Enabled)
            preferenceRepository.setHasCompletedOnBoarding(HasCompletedOnBoarding.Completed)
            viewModel.onMainButtonClick(OnBoardingPageName.Fingerprint, ContextHolder(None))
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial.copy(isCompleted = true))
        }
    }

    @Test
    fun `given a click on skip fingerprint should complete on boarding`() = runTest {
        viewModel.onBoardingUiState.test {
            skipItems(1)
            preferenceRepository.setHasCompletedOnBoarding(HasCompletedOnBoarding.Completed)
            viewModel.onSkipButtonClick(OnBoardingPageName.Fingerprint)
            assertThat(awaitItem()).isEqualTo(OnBoardingUiState.Initial.copy(isCompleted = true))
        }
    }
}
