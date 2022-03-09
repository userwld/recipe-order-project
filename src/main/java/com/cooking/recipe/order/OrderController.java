package com.cooking.recipe.order;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cooking.recipe.order.service.IOrderService;

@Controller
public class OrderController {
	@Autowired IOrderService service;
	
	/* 상품상세 페이지에서 바로주문 눌렀을 때 */
	@RequestMapping(value="/orderNowProc")	
	public String orderNowProc(String productNum, String productName, String price, String amount, Model model) {
		boolean check = service.orderNowProc(productNum,productName,price,amount,model);
		if(check == false) return "forward:join";
		return "forward:order";
	}
	
	/* 장바구니 페이지에서 주문하기 눌렀을 때 -> 카트에 담긴거 전부 가져와서 출력*/
	@RequestMapping(value="/orderCartProc")
	public String orderCartProc(String[] orderItems, Model model) {
		boolean check = service.orderCart(orderItems, model);
		if(check == false) return "forward:join";
		return "forward:order";
	}
	
	@RequestMapping(value = "/payProc", produces = "application/json; charset=utf-8")
	@ResponseBody
	public String payProc(@RequestBody Map<String,String> map){
		return service.payProc(map);
	}
	
	/* 결제 준비 완료 후 결제 승인으로 리다이렉트 됐을때*/
	@RequestMapping(value="/payApprove")
	public String payApprove(String pg_token, String state) {
		String resURL = "";
		
		// 결제 승인
		int resCode = service.payApprove(pg_token);

		if(resCode == 200) {
			service.addrUpdate();		// 결제 성공시 member테이블에 이름과 배송지 정보 업데이트
			
			if(state.equals("now")) {	// 바로 주문 결제 성공시
				service.orderInsert(); 	// 단건 주문 주문테이블에 삽입
				service.stockUpdate();	// 단건 주문 상품 -> 상품테이블에서 재고 수정
				
			}else {						// 장바구니 페이지 담았던것 주문 결제 성공시
				service.ordersInsert();	// 여러건 주문 주문테이블에 삽입
				service.stocksUpdate();	// 여러건 주문 상품 -> 상품테이블에서 재고 수정
				service.cartDelete();	// 주문한 상품들 장바구니테이블에서 삭제
			}
						
			resURL = "forward:orderHistoryViewProc";
		}else {
			resURL = "forward:orderDetailViewProc";
		}
				
		return resURL;
		
	}
	
	/* 결제 준비 후 결제 실패로 리다이렉트 시*/
	@RequestMapping(value="/payFail")
	public String payFail(String state) {
		if(state.equals("now")) {
			String[] param = service.payFail();		// 바로주문에서 결제 실패시 -> 다시 바로주문 페이지로
			return "forward:orderNowProc?productNum="+param[0]+"&productName="+param[1]+"&price="+param[2]+"&amount="+param[3];
		}else return "forward:cartViewProc";		// 장바구니 결제 실패시 -> 다시 장바구니 페이지로
		
	}
	
	/* 결제 준비 후 결제 취소로 리다이렉트 시*/
	@RequestMapping(value="/payCancel")
	public String payCancel(String state) {
		if(state.equals("now")) {
			String[] param = service.payFail();		// 바로주문에서 결제 취소시
			return "forward:orderNowProc?productNum="+param[0]+"&productName="+param[1]+"&price="+param[2]+"&amount="+param[3];
		}else return "forward:cartViewProc";		// 장바구니 결제 실패시 -> 다시 장바구니 페이지로
	}
	
	@RequestMapping(value="/orderDetailViewProc")
	public String orderDetailViewProc() {
		return "forward:index?formpath=orderDetail";
	}
	
	@RequestMapping(value="/orderHistoryViewProc")
	public String orderHistoryViewProc(){
		return "forward:index?formpath=orderHistory";
	}
	
}
