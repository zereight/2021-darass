import { Link } from "react-router-dom";
import styled from "styled-components";
import { pageMaxWidth } from "../../../styles/constants";
import { PALETTE } from "../../../styles/palette";

const Container = styled.nav`
  width: 100%;
  display: flex;
  justify-content: center;
  background-color: ${PALETTE.PRIMARY};
  padding: 1rem 2.5rem;
`;

const Wrapper = styled.div`
  width: 100%;
  max-width: ${pageMaxWidth};
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

const NavLink = styled(Link)`
  margin-left: 0.8rem;
  font-size: 2.4rem;
  font-weight: 800;
  color: ${PALETTE.WHITE};
  line-height: 40px;
`;

export { Container, Wrapper, NavLink };