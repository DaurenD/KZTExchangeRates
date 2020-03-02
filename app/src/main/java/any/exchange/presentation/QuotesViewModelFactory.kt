package any.exchange.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import any.exchange.domain.GetQuotesListUseCase

class QuotesViewModelFactory(private val getQuotesListUseCase: GetQuotesListUseCase) :
    ViewModelProvider.Factory {


    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return QuotesViewModel(getQuotesListUseCase) as T
    }

}