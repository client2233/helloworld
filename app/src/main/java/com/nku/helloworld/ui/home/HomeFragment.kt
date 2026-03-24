package com.nku.helloworld.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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

        // 让背景上沿精确对齐到搜索框下沿。
        binding.searchBar.post {
            val lp = binding.studyPlanBackground.layoutParams as MarginLayoutParams
            lp.topMargin = binding.searchBar.bottom
            binding.studyPlanBackground.layoutParams = lp
        }

        // 配置底部面板可拖拽行为
        val cardView = binding.bottomSheetCard
        val behavior = BottomSheetBehavior.from(cardView)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isDraggable = true
        behavior.isHideable = false

        val density = requireContext().resources.displayMetrics.density
        val extraVisible = (52 * density).toInt()
        
        // 折叠到底时，完整显示“学习计划/打卡提醒”两张统计卡片。
        binding.bottomSheetCard.post {
            val scrollPadding = binding.homeCardScroll.paddingTop + binding.homeCardScroll.paddingBottom
            val cardsHeight = binding.statsCardsRow.height
            behavior.peekHeight = cardsHeight + scrollPadding + extraVisible
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
        }

        // 将内部滚动限制在“统计卡片完整露出”的边界，禁止继续向下浏览。
        binding.homeCardScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val maxScrollY = computeMaxStatsScrollY()
            if (scrollY > maxScrollY) {
                binding.homeCardScroll.post { binding.homeCardScroll.scrollTo(0, maxScrollY) }
            }
        }

        // 展开时停在学习计划区域，收起到底时只露出统计卡片下沿。
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.homeCardScroll.post {
                            binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
                        }
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
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
                    binding.homeCardScroll.scrollTo(0, computeMaxStatsScrollY())
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
}