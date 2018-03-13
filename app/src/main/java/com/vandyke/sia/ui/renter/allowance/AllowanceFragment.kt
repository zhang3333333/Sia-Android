/*
 * Copyright (c) 2017 Nicholas van Dyke. All rights reserved.
 */

package com.vandyke.sia.ui.renter.allowance

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.vandyke.sia.R
import com.vandyke.sia.dagger.SiaViewModelFactory
import com.vandyke.sia.data.local.Prefs
import com.vandyke.sia.data.siad.SiadStatus
import com.vandyke.sia.getAppComponent
import com.vandyke.sia.ui.common.BaseFragment
import com.vandyke.sia.ui.renter.allowance.AllowanceViewModel.Currency
import com.vandyke.sia.ui.renter.allowance.AllowanceViewModel.Metrics.*
import com.vandyke.sia.util.*
import com.vandyke.sia.util.rx.observe
import kotlinx.android.synthetic.main.fragment_allowance.*
import javax.inject.Inject

// TODO: handle when there's no initial data from the db and the node isn't running yet. i.e. if you call setAllowance it'll crash
// because it'll have null values
class AllowanceFragment : BaseFragment() {
    override val layoutResId = R.layout.fragment_allowance
    override val hasOptionsMenu = true
    override val title: String = "Allowance"

    @Inject
    lateinit var factory: SiaViewModelFactory
    private lateinit var vm: AllowanceViewModel

    @Inject
    lateinit var siadStatus: SiadStatus

    private var highlightedX = -1f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context!!.getAppComponent().inject(this)

        vm = ViewModelProviders.of(this, factory).get(AllowanceViewModel::class.java)

        allowanceSwipe.setOnRefreshListener { vm.refresh() }
        allowanceSwipe.setColors(context!!)

        /* chart setup */
        val dataSet = PieDataSet(listOf(
                PieEntry(1f, context!!.getDrawable(R.drawable.ic_cloud_upload)),
                PieEntry(1f, context!!.getDrawable(R.drawable.ic_cloud_download)),
                PieEntry(1f, context!!.getDrawable(R.drawable.ic_storage)),
                PieEntry(1f, context!!.getDrawable(R.drawable.ic_file)),
                PieEntry(1f, context!!.getDrawable(R.drawable.ic_money))),
                "Spending")
        val colors = ColorTemplate.MATERIAL_COLORS.toMutableList()
        colors.add(0, context!!.getColorRes(android.R.color.holo_purple))
        dataSet.colors = colors
        dataSet.sliceSpace = 1.5f

        val data = PieData(dataSet)
        data.setDrawValues(false)

        pieChart.apply {
            this.data = data
            isRotationEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = false
            // the listener is called twice when an item is touched. Not sure why
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, h: Highlight) {
                    vm.currentMetric.value = AllowanceViewModel.Metrics.values()[h.x.toInt()]
                }

                override fun onNothingSelected() {
                    pieChart.highlightValue(highlightedX, 0)
                }
            })

            invalidate()
        }

        /* metric spinner setup */
        val metricAdapter = ArrayAdapter<String>(context, R.layout.spinner_selected_item)
        metricAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        metricAdapter.addAll(AllowanceViewModel.Metrics.values().map { it.text })
        metricSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                vm.currentMetric.value = AllowanceViewModel.Metrics.values()[position]
            }
        }
        metricSpinner.adapter = metricAdapter

        /* listeners for clicky stuff in settings */
        fundsClickable.setOnClickListener {
            DialogUtil.editTextDialog(context!!,
                    "Funds",
                    "Set",
                    { vm.setAllowance(it.text.toString().toBigDecimal().toHastings()) },
                    "Cancel",
                    editTextFunc = { hint = "Amount (SC)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL })
                    .showDialogAndKeyboard()
        }

        hostsClickable.setOnClickListener {
            DialogUtil.editTextDialog(context!!,
                    "Hosts",
                    "Set",
                    { vm.setAllowance(hosts = it.text.toString().toInt()) },
                    "Cancel",
                    editTextFunc = { hint = "Hosts"; inputType = InputType.TYPE_CLASS_NUMBER })
                    .showDialogAndKeyboard()
        }

        periodClickable.setOnClickListener {
            DialogUtil.editTextDialog(context!!,
                    "Period",
                    "Set",
                    { vm.setAllowance(period = it.text.toString().toInt()) },
                    "Cancel",
                    editTextFunc = { hint = "Blocks"; inputType = InputType.TYPE_CLASS_NUMBER })
                    .showDialogAndKeyboard()
        }

        renewWindowClickable.setOnClickListener {
            DialogUtil.editTextDialog(context!!,
                    "Renew window",
                    "Set",
                    { vm.setAllowance(renewWindow = it.text.toString().toInt()) },
                    "Cancel",
                    editTextFunc = { hint = "Blocks"; inputType = InputType.TYPE_CLASS_NUMBER })
                    .showDialogAndKeyboard()
        }

        /* viewModel observation */
        vm.refreshing.observe(this) {
            allowanceSwipe.isRefreshing = it
        }

        vm.allowance.observe(this) {
            // TODO: show day equivalents of block values
                fundsValue.text = it.funds.toSC().format() + " SC"
                hostsValue.text = it.hosts.format()
                periodValue.text = it.period.format() + " blocks"
                renewWindowValue.text = it.renewwindow.format() + " blocks"
        }

        vm.currentMetric.observe(this) {
            val x = it.ordinal.toFloat()
            if (dataSet.entryCount != 0 && highlightedX != x) {
                highlightedX = x
                pieChart.highlightValue(x, 0)
            }

            metricSpinner.setSelection(it.ordinal)
        }

        vm.currentMetricValues.observe(this) { (price, spent, purchasable) ->
            val currency = " " + if (vm.currency.value == Currency.SC) "SC" else Prefs.fiatCurrency
            val metric = vm.currentMetric.value

            estPriceHeader.visibleIf(metric != UNSPENT)
            tvPrice.visibleIf(metric != UNSPENT)
            purchasableHeader.visibleIf(metric != UNSPENT)
            tvPurchaseable.visibleIf(metric != UNSPENT)
            if (metric == UNSPENT) {
                spentHeader.text = "Remaining funds"
                tvSpent.text = spent.toSC().format() + currency
            } else {
                spentHeader.text = "Spent"
                if (metric == STORAGE) {
                    estPriceHeader.text = "Est. price/TB/month"
                    purchasableHeader.text = "Purchasable (1 month)"
                    tvPurchaseable.text = purchasable.format() + " TB"
                } else if (metric == UPLOAD || metric == DOWNLOAD) {
                    estPriceHeader.text = "Est. price/TB"
                    purchasableHeader.text = "Purchasable"
                    tvPurchaseable.text = purchasable.format() + " TB"
                } else if (metric == CONTRACT) {
                    estPriceHeader.text = "Est. price"
                    purchasableHeader.text = "Purchasable"
                    tvPurchaseable.text = purchasable.format()
                }

                tvPrice.text = price.toSC().format() + currency
                tvSpent.text = spent.toSC().format() + currency
            }
        }

        vm.spending.observe(this) {
            // TODO: use some Math.max or min here so that when they're all zero, there's still some data,
            // and so that ones that are zero can still be seen when the others aren't zero
            dataSet.values[0].y = it.uploadspending.toSC().toFloat()
            dataSet.values[1].y = it.downloadspending.toSC().toFloat()
            dataSet.values[2].y = it.storagespending.toSC().toFloat()
            dataSet.values[3].y = it.contractspending.toSC().toFloat()
            dataSet.values[4].y = it.unspent.toSC().toFloat()
            dataSet.notifyDataSetChanged()

            // should I be doing this here too?
//            val x = vm.currentMetric.value.ordinal.toFloat()
//            if (highlightedX != x) {
//                highlightedX = x
//                pieChart.highlightValue(x, 0)
//            }

            pieChart.notifyDataSetChanged()
            pieChart.invalidate()
        }

        vm.error.observe(this) {
            it.snackbar(view)
        }

        siadStatus.state.observe(this) {
            if (it == SiadStatus.State.SIAD_LOADED)
                vm.refresh()
        }
    }

    override fun onShow() {
        vm.refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_allowance, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.currency_button -> vm.toggleDisplayedCurrency()
            else -> return false
        }
        return true
    }
}