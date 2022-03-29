package com.controller;

import com.dao.StoreupDao;
import com.dao.ZhaopinxinxiDao;
import com.entity.StoreupEntity;
import com.entity.ZhaopinxinxiEntity;
import com.service.YonghuService;
import com.service.ZhaopinxinxiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
public class tuijianController {
    //返回tuijian.html
    @RequestMapping("gettuijian")
    public String gettuijian(){
        return "tuijian";
    }

    @Autowired
    YonghuService yonghuService;
    @Autowired
    StoreupDao storeupDao;
    @Autowired
    ZhaopinxinxiDao zhaopinxinxiDao;
    @Autowired
    ZhaopinxinxiService zhaopinxinxiService;
    //协同过滤推荐算法
    @RequestMapping("/tuijian")
    @ResponseBody
    public List<ZhaopinxinxiEntity> tuijian(HttpServletRequest request) {
        //从session获取当前登录用户的id
        Long orginal = (Long)request.getSession().getAttribute("userId");
        System.out.println(orginal);
        //获取收藏表的所有数据
        List<StoreupEntity> users = storeupDao.selectList(null);
        //把获取的所有数据存入HashMap
        HashMap<Long, List<Long>> userRecommend = new HashMap();
        for (int i = 0; i < users.size(); i++) {
            // 获取每一个用户和岗位Id
            Long userid = users.get(i).getUserid();
            Long refid = users.get(i).getRefid();

            // 如果推荐map中有以当前用户为Key的数据
            if (userRecommend.containsKey(userid)) { //第一次循环，这个map是新建的，肯定都是Null，所以userRecommend肯定没有以当前用户id作为key的值
                // 根据用户id获取map对应的value
                List<Long> recommendTemp = userRecommend.get(userid);
                // 在拿到的集合中添加新的岗位id
                recommendTemp.add(refid);
                // 更新此key value
                userRecommend.put(userid, recommendTemp);
            } else {
                // 如果不包含，新建一个集合，然后将Key value放入map
                List<Long> recommendTemp = new ArrayList<>();  //没有值就新建一个List
                recommendTemp.add(refid);					  // 添加当前循环的这个refid,这个集合的数据就是 [2]
                userRecommend.put(userid, recommendTemp);         // 将当前循环的userid作为key，集合作为value放入userRecommend中，此时map中的值为｛1，[2]｝
            }
        }
        System.out.println(userRecommend);
        // 新建我的岗位列表
        List<Long> myRecommend = new ArrayList<>();

        // 如果刚才存放的map中包含有以当前登录用户id为key的数据
        if (userRecommend.containsKey(orginal)) {
            myRecommend = userRecommend.get(orginal);
        } else {
            myRecommend = new ArrayList<>();
        }

        // 将我的岗位列表集合转换为set集合
        HashSet<Long> myRecommendSet = new HashSet<Long>(myRecommend);
        double maxValue = 0;
        long maxId = -1;

        for (long key : userRecommend.keySet()) {
            // 当遍历到当前用户Id为key的时候，跳过此次循环，开始下一次循环
            if (key == orginal) {
                continue;
            }
            // 根据当前循环到的key获取value
            List<Long> thisRecommend = userRecommend.get(key);
            // 将当前的集合转换为set
            HashSet<Long> thisRecommendSet = new HashSet<>(thisRecommend);
            // 将我的集合转换为set
            HashSet<Long> intersection = new HashSet<>(myRecommendSet);
            // 取交集 我的岗位会剩下两个集合中都有的岗位id
            intersection.retainAll(thisRecommendSet);
            HashSet<Long> union = new HashSet<>(myRecommendSet);

            // 将两个人收藏的所有岗位去重并放入此集合中
            union.addAll(thisRecommendSet);

            // 如果取过交集的集合为空，说明当前循环到的用户和当前登录的用户没有收藏同一种歌曲，跳过此次循环，开始下一次循环
            if (union.size() == 0) {
                continue;
            } else {
                // 如果不为空，则用我的岗位列表/交集集合
                double ratio = (double) intersection.size() / union.size();
                if (ratio > maxValue) {
                    // 最大值就位两者之比
                    maxValue = ratio;
                    // maxId = 当前循环的用户
                    maxId = key;
                }
            }
        }


        // 创建岗位推荐列表
        List<Long> MovieRecommendList = new ArrayList<>();
        //　如果maxId没有被更改过，则为当前登录用户ID
        if (maxId == -1) { //此时maxId = 2
            maxId = orginal;
        } else {
            // 如果被更改过，就从推荐列表中取出key为maxId（maxId为拥有最大交集的用户id） 的岗位列表，
            HashSet<Long> differenceTemp = new HashSet<>(userRecommend.get(maxId)); // differenceTemp = [2,3,4]
            // maxId用户岗位列表中的岗位 - 我的岗位列表中的岗位 = 我没有的岗位
            differenceTemp.removeAll(myRecommendSet); // differenceTemp = [4] 所以，在推荐列表中就会出现id为4的岗位
            if(differenceTemp.isEmpty()) {
                differenceTemp.addAll(userRecommend.get(maxId));
            }
            MovieRecommendList = new ArrayList<Long>(differenceTemp);

        }
        System.out.println(MovieRecommendList);


        ArrayList<ZhaopinxinxiEntity> list = new ArrayList<>();
        for (int i = 0; i < MovieRecommendList.size(); i++) {
            ZhaopinxinxiEntity zhaopinxinxiEntity1 = zhaopinxinxiService.selectById(MovieRecommendList.get(i));
            list.add(zhaopinxinxiEntity1);
        }
        return list;
    }
}
