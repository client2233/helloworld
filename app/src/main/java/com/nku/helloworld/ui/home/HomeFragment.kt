package com.nku.helloworld.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.doOnLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nku.helloworld.R
import com.nku.helloworld.databinding.FragmentHomeBinding

import com.google.android.material.bottomsheet.BottomSheetBehavior

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var isScrollClamped = true
    private var hasAppliedNonOverlapLayout = false
    private var sheetAnchorTop = 0

    private fun computeMaxStatsScrollY(): Int {
        val viewportHeight = binding.homeCardScroll.height - binding.homeCardScroll.paddingBottom
        return (binding.statsCardsRow.bottom - viewportHeight).coerceAtLeast(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val items = listOf(
            HomeRecentItem("英语单词打卡", getString(R.string.home_category_study), "07:30", R.color.status_todo),
            HomeRecentItem("晚间复盘打卡", getString(R.string.home_category_study), "21:30", R.color.status_pending),
            HomeRecentItem("晨读完成记录", getString(R.string.home_category_study), "06:40", R.color.status_done),
        )
        binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerView.adapter = HomeRecentAdapter(items)

        val topBarBasePadding = binding.homeTopBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeTopBar) { view, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                topBarBasePadding + statusBarTop,
                view.paddingRight,
                view.paddingBottom
            )
            binding.root.post {
                // Insets changed: re-anchor the sheet to keep search bar fully unobstructed.
                applyNonOverlapAnchors(behavior = BottomSheetBehavior.from(binding.bottomSheetCard))
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.homeTopBar)
        binding.homeTopBar.bringToFront()

        // 配置底部面板可拖拽行为
        val cardView = binding.bottomSheetCard
        val behavior = BottomSheetBehavior.from(cardView)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isDraggable = true
        behavior.isHideable = false
        behavior.isFitToContents = true

        val density = requireContext().resources.displayMetrics.density
        val extraVisible = (52 * density).toInt()

        binding.searchBar.doOnLayout {
            applyNonOverlapAnchors(behavior)
        }
        
        // 折叠到底时，完整显示“学习计划/打卡提醒”两张统计卡片。
        binding.bottomSheetCard.post {
            applyNonOverlapAnchors(behavior)
            val scrollPadding = binding.homeCardScroll.paddingTop + binding.homeCardScroll.paddingBottom
            val cardsHeight = binding.statsCardsRow.height
            val parentHeight = (binding.bottomSheetCard.parent as View).height
            val maxByAnchor = (parentHeight - sheetAnchorTop).coerceAtLeast(1)
            val maxPeekHeight = binding.bottomSheetCard.height.coerceAtLeast(1).coerceAtMost(maxByAnchor)
            behavior.peekHeight = (cardsHeight + scrollPadding + extraVisible)
                .coerceAtMost(maxPeekHeight)
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
        }

        // 仅在收起态限制内部滚动，展开后允许列表继续向下滚动。
        binding.homeCardScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val maxScrollY = computeMaxStatsScrollY()
            if (isScrollClamped && scrollY > maxScrollY) {
                binding.homeCardScroll.post { binding.homeCardScroll.scrollTo(0, maxScrollY) }
            }
        }

        // 展开时停在学习计划区域，收起到底时只露出统计卡片下沿。
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        isScrollClamped = false
                        binding.homeCardScroll.post {
                            binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
                        }
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        isScrollClamped = true
                        binding.homeCardScroll.post {
                            // 收起到底固定到统计卡片完整可见的位置。
                            binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
                        }
                    }

                    BottomSheetBehavior.STATE_DRAGGING,
                    BottomSheetBehavior.STATE_SETTLING,
                    BottomSheetBehavior.STATE_HALF_EXPANDED,
                    BottomSheetBehavior.STATE_HIDDEN -> Unit

                    else -> Unit
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // 接近收起位置时保持顶部锚点，避免内部滚动残留导致卡片被挡住。
                if (slideOffset <= 0.12f) {
                    isScrollClamped = true
                    binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
                } else {
                    isScrollClamped = false
                }
            }
        })

        // 首次进入默认收起到底部，露出背景区域。
        binding.homeCardScroll.post {
            binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyNonOverlapAnchors(behavior: BottomSheetBehavior<*>) {
        val density = resources.displayMetrics.density
        val safetyGapPx = (10f * density).toInt()
        val anchorTop = (binding.homeTopBar.bottom + safetyGapPx).coerceAtLeast(binding.searchBar.bottom + safetyGapPx)
        if (anchorTop <= 0) return
        sheetAnchorTop = anchorTop

        val backgroundLp = binding.studyPlanBackground.layoutParams as MarginLayoutParams
        if (backgroundLp.topMargin != binding.searchBar.bottom) {
            backgroundLp.topMargin = binding.searchBar.bottom
            binding.studyPlanBackground.layoutParams = backgroundLp
        }

        val sheetLp = binding.bottomSheetCard.layoutParams as MarginLayoutParams
        if (sheetLp.topMargin != anchorTop) {
            sheetLp.topMargin = anchorTop
            binding.bottomSheetCard.layoutParams = sheetLp
        }

        behavior.expandedOffset = anchorTop
        if (!hasAppliedNonOverlapLayout) {
            hasAppliedNonOverlapLayout = true
            binding.bottomSheetCard.requestLayout()
        }
    }
}